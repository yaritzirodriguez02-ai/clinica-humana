package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.NotificacionCita;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.repository.NotificacionCitaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Envía al tutor (o paciente adulto) la confirmación de su cita y el recordatorio
 * del día de la consulta por SMS y WhatsApp, usando la API REST de Twilio.
 *
 * <p>Si todavía no hay credenciales configuradas en {@code application.properties},
 * los mensajes NO se pierden: se guardan en la bitácora como {@code SIMULADO} y se
 * escriben completos en el log, para poder revisar el texto y probar el flujo.</p>
 *
 * <p>Nunca lanza excepciones hacia el agendamiento: un fallo del proveedor se registra
 * como {@code ERROR} y la cita queda agendada de todos modos.</p>
 */
@Service
public class NotificacionCitaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCitaService.class);

    private static final String DIRECCION =
            "Av. Lázaro Cárdenas #59, 3er piso, Col. Haciendita, Chilpancingo, Gro.";
    private static final String LLEGADA_ANTICIPADA = "20 minutos antes";

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern SID_TWILIO = Pattern.compile("\"sid\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${notificaciones.habilitadas:true}")
    private boolean habilitadas;

    @Value("${notificaciones.sms.habilitado:true}")
    private boolean smsHabilitado;

    @Value("${notificaciones.whatsapp.habilitado:true}")
    private boolean whatsappHabilitado;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.sms.from:}")
    private String smsFrom;

    @Value("${twilio.whatsapp.from:}")
    private String whatsappFrom;

    @Autowired
    private NotificacionCitaRepository notificacionRepo;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** Envía la confirmación inmediata después de agendar. Devuelve el resumen de canales usados. */
    public String confirmar(Cita cita) {
        return enviar(cita, NotificacionCita.TIPO_CONFIRMACION);
    }

    /** Envía el recordatorio del día de la cita. Devuelve el resumen de canales usados. */
    public String recordar(Cita cita) {
        return enviar(cita, NotificacionCita.TIPO_RECORDATORIO);
    }

    /** Indica si ya hay credenciales reales de Twilio; si no, los mensajes quedan en modo de prueba. */
    public boolean estaConfigurado() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && ((smsFrom != null && !smsFrom.isBlank()) || (whatsappFrom != null && !whatsappFrom.isBlank()));
    }

    private String enviar(Cita cita, String tipo) {
        if (cita == null) {
            return "";
        }
        if (!habilitadas) {
            log.info("Notificaciones deshabilitadas: no se envía {} de la cita #{}", tipo, cita.getIdCita());
            return "";
        }

        String telefono = telefonoDestino(cita);
        if (telefono == null) {
            log.warn("Cita #{} sin teléfono de contacto válido: no se puede notificar.", cita.getIdCita());
            return "";
        }

        List<String> enviados = new ArrayList<>();
        boolean quiereWhatsapp = Boolean.TRUE.equals(cita.getNotificarWhatsapp());
        boolean quiereSms = Boolean.TRUE.equals(cita.getNotificarSms());

        if (whatsappHabilitado && quiereWhatsapp) {
            if (enviarPorCanal(cita, tipo, NotificacionCita.CANAL_WHATSAPP, telefono, construirMensaje(cita, tipo))) {
                enviados.add("WhatsApp");
            }
        }
        if (smsHabilitado && quiereSms) {
            if (enviarPorCanal(cita, tipo, NotificacionCita.CANAL_SMS, telefono, construirMensaje(cita, tipo))) {
                enviados.add("SMS");
            }
        }
        return String.join(" y ", enviados);
    }

    /**
     * Envía un mensaje por el canal indicado y lo registra en la bitácora.
     * @return true si el proveedor lo aceptó (o si quedó simulado por falta de credenciales).
     */
    private boolean enviarPorCanal(Cita cita, String tipo, String canal, String telefono, String mensaje) {
        NotificacionCita registro = new NotificacionCita();
        registro.setCita(cita);
        registro.setTipo(tipo);
        registro.setCanal(canal);
        registro.setTelefono(telefono);
        registro.setMensaje(mensaje);

        String from = NotificacionCita.CANAL_WHATSAPP.equals(canal) ? whatsappFrom : smsFrom;
        String destino = NotificacionCita.CANAL_WHATSAPP.equals(canal) ? "whatsapp:+" + telefono : "+" + telefono;

        if (from == null || from.isBlank() || accountSid == null || accountSid.isBlank()
                || authToken == null || authToken.isBlank()) {
            registro.setEstado(NotificacionCita.ESTADO_SIMULADO);
            notificacionRepo.save(registro);
            log.info("[{} SIMULADO] Cita #{} → +{}\n{}", canal, cita.getIdCita(), telefono, mensaje);
            return true;
        }

        try {
            String cuerpo = "To=" + encode(destino)
                    + "&From=" + encode(from.trim())
                    + "&Body=" + encode(mensaje);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/"
                            + accountSid.trim() + "/Messages.json"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            (accountSid.trim() + ":" + authToken.trim()).getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
                Matcher m = SID_TWILIO.matcher(respuesta.body());
                registro.setEstado(NotificacionCita.ESTADO_ENVIADO);
                registro.setDetalleError(m.find() ? "sid=" + m.group(1) : null);
                notificacionRepo.save(registro);
                log.info("{} enviado de la cita #{} a +{}", canal, cita.getIdCita(), telefono);
                return true;
            }

            registro.setEstado(NotificacionCita.ESTADO_ERROR);
            registro.setDetalleError(recortar("HTTP " + respuesta.statusCode() + ": " + respuesta.body()));
            notificacionRepo.save(registro);
            log.warn("{} rechazado por Twilio para la cita #{}: HTTP {} {}",
                    canal, cita.getIdCita(), respuesta.statusCode(), respuesta.body());
            return false;

        } catch (Exception e) {
            registro.setEstado(NotificacionCita.ESTADO_ERROR);
            registro.setDetalleError(recortar(e.getClass().getSimpleName() + ": " + e.getMessage()));
            notificacionRepo.save(registro);
            log.warn("No se pudo enviar {} de la cita #{}: {}", canal, cita.getIdCita(), e.getMessage());
            return false;
        }
    }

    /** Texto completo con todos los datos registrados, la especialidad y la doctora asignada. */
    String construirMensaje(Cita cita, String tipo) {
        Paciente paciente = cita.getPaciente();
        Tutor tutor = paciente != null ? paciente.getTutor() : null;

        String nombrePaciente = paciente != null
                ? (paciente.getNombre() + " " + nvl(paciente.getApellidos())).trim()
                : "(sin paciente)";
        String curp = paciente != null && paciente.getCurp() != null ? paciente.getCurp() : "(sin CURP)";
        String nombreTutor = tutor != null
                ? (tutor.getNombre() + " " + nvl(tutor.getApellidos())).trim()
                : "No registrado";
        String telefono = telefonoDestino(cita);

        LocalTime llegada = cita.getHora() != null ? cita.getHora().minusMinutes(20) : null;
        String fecha = cita.getFecha() != null ? capitalizar(cita.getFecha().format(FECHA_LARGA)) : "(sin fecha)";
        String hora = cita.getHora() != null ? cita.getHora().format(HORA) + " hrs" : "(sin hora)";
        String horaLlegada = llegada != null ? llegada.format(HORA) + " hrs" : "(por definir)";
        String especialidad = "ODONTOLOGIA".equalsIgnoreCase(cita.getEspecialidad()) ? "Odontología" : "Pediatría";
        String doctora = cita.getMedico() != null ? nvl(cita.getMedico().getNombreCompleto()) : "(por asignar)";
        String motivo = nvl(cita.getMotivo());

        StringBuilder texto = new StringBuilder();
        if (NotificacionCita.TIPO_RECORDATORIO.equals(tipo)) {
            texto.append("RECORDATORIO: HOY TIENES CITA EN CLÍNICA HUMANA\n\n");
        } else {
            texto.append("CITA CONFIRMADA - CLÍNICA HUMANA\n\n");
        }
        texto.append("Folio: #").append(cita.getIdCita()).append('\n');
        texto.append("Paciente: ").append(nombrePaciente).append('\n');
        texto.append("CURP: ").append(curp).append('\n');
        if (tutor != null) {
            texto.append("Tutor: ").append(nombreTutor).append(" (").append(nvl(tutor.getParentesco())).append(")\n");
        }
        texto.append("Contacto: ").append(telefonoLocal(telefono)).append('\n');
        texto.append("Especialidad: ").append(especialidad).append('\n');
        texto.append("Doctora asignada: ").append(doctora).append('\n');
        texto.append("Fecha: ").append(fecha).append(", ").append(hora).append('\n');
        texto.append("Motivo: ").append(motivo).append('\n');
        texto.append("Llega ").append(LLEGADA_ANTICIPADA)
                .append(" (").append(horaLlegada).append(")\n");
        texto.append("Dirección: ").append(DIRECCION).append('\n');
        if (NotificacionCita.TIPO_RECORDATORIO.equals(tipo)) {
            texto.append("\nSi no puedes asistir, avisa a la clínica.");
        } else {
            texto.append("\nTe enviaremos un recordatorio el día de tu cita.");
        }
        return texto.toString();
    }

    /** Teléfono del tutor (o del paciente adulto) normalizado a 10 dígitos, con lada 52. */
    private String telefonoDestino(Cita cita) {
        if (cita.getPaciente() == null || cita.getPaciente().getTutor() == null) {
            return null;
        }
        String raw = cita.getPaciente().getTutor().getTelefono();
        if (raw == null) {
            return null;
        }
        String digitos = raw.replaceAll("[^0-9]", "");
        if (digitos.length() == 10) {
            return "52" + digitos;
        }
        if (digitos.length() == 12 && digitos.startsWith("52")) {
            return digitos;
        }
        return null;
    }

    /** Número de 10 dígitos tal como lo conoce el paciente (sin la lada 52). */
    private static String telefonoLocal(String telefono) {
        if (telefono == null) {
            return "-";
        }
        return telefono.length() == 12 && telefono.startsWith("52") ? telefono.substring(2) : telefono;
    }

    private static String encode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String nvl(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor.trim();
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 250 ? texto.substring(0, 250) : texto;
    }
}
