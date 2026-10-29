package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.InformeOdontologico;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.InformeOdontologicoRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import com.clinicahumana.clinica_humana.service.PacienteService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import com.clinicahumana.clinica_humana.validation.ValidacionesClinicas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Módulo de informes en hoja membretada de CISO.
 *
 * Flujo: la odontóloga (o recepción, o el administrador) elige al paciente,
 * redacta el informe y lo imprime en la hoja oficial tamaño carta. Cada informe
 * queda guardado con folio y puede reimprimirse desde el historial.
 */
@Controller
@RequestMapping("/informes")
public class InformeController {

    private static final DateTimeFormatter FORMATO_FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));

    private static final List<String> SEDES = List.of("Chilpancingo, Gro.", "Iguala, Gro.");

    @Autowired
    private InformeOdontologicoRepository informeRepo;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private ExpedienteOdontopediatricoRepository expOdontoRepo;

    // ===================== HISTORIAL =====================

    @GetMapping
    public String listar(@RequestParam(name = "buscar", required = false) String buscar, Model model) {
        List<InformeOdontologico> informes = informeRepo.findAllByOrderByFechaEmisionDescIdInformeDesc();

        if (buscar != null && !buscar.trim().isEmpty()) {
            String termino = buscar.trim().toLowerCase();
            List<InformeOdontologico> filtrados = new ArrayList<>();
            for (InformeOdontologico i : informes) {
                String paciente = (i.getPaciente().getNombre() + " " + i.getPaciente().getApellidos()).toLowerCase();
                if (i.getAsunto().toLowerCase().contains(termino)
                        || paciente.contains(termino)
                        || i.getTipoEtiqueta().toLowerCase().contains(termino)
                        || i.getFolio().toLowerCase().contains(termino)) {
                    filtrados.add(i);
                }
            }
            informes = filtrados;
        }

        model.addAttribute("informes", informes);
        model.addAttribute("buscar", buscar);
        return "informes/lista";
    }

    // ===================== CAPTURA =====================

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(name = "paciente", required = false) Long idPaciente,
                        @RequestParam(name = "cita", required = false) Long idCita,
                        Principal principal,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        InformeOdontologico informe = new InformeOdontologico();
        informe.setFechaEmision(LocalDate.now());

        if (idPaciente != null) {
            Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
            if (paciente == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
                return "redirect:/informes";
            }
            informe.setPaciente(paciente);

            if (idCita != null) {
                citaRepo.findById(idCita)
                        .filter(c -> c.getPaciente().getIdPaciente().equals(paciente.getIdPaciente()))
                        .ifPresent(informe::setCita);
            }
        }

        informe.setMedico(resolverSuscriptor(principal, informe.getCita()));
        prepararFormulario(model, informe);
        return "informes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        InformeOdontologico informe = informeRepo.findById(id).orElse(null);
        if (informe == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El informe solicitado no existe.");
            return "redirect:/informes";
        }

        prepararFormulario(model, informe);
        return "informes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("informe") InformeOdontologico informe,
                          @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                          @RequestParam(name = "idCita", required = false) Long idCita,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        InformeOdontologico existente = informe.getIdInforme() != null
                ? informeRepo.findById(informe.getIdInforme()).orElse(null)
                : null;

        Paciente paciente = existente != null
                ? existente.getPaciente()
                : (idPaciente != null ? pacienteService.buscarPorId(idPaciente).orElse(null) : null);

        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Selecciona un paciente registrado para redactar el informe.");
            return "redirect:/informes";
        }

        informe.setPaciente(paciente);
        informe.setTipo(InformeOdontologico.TIPOS.containsKey(informe.getTipo()) ? informe.getTipo() : "INFORME_CLINICO");
        informe.setLugar(SEDES.contains(informe.getLugar()) ? informe.getLugar() : SEDES.get(0));

        if (informe.getDestinatario() == null || informe.getDestinatario().trim().isEmpty()) {
            informe.setDestinatario("A QUIEN CORRESPONDA");
        } else {
            informe.setDestinatario(informe.getDestinatario().trim());
        }

        // Consulta de referencia: debe pertenecer al paciente del informe
        Cita cita = null;
        if (idCita != null) {
            cita = citaRepo.findById(idCita)
                    .filter(c -> c.getPaciente().getIdPaciente().equals(paciente.getIdPaciente()))
                    .orElse(null);
        }
        informe.setCita(cita);

        // El suscriptor se define antes de validar: el documento siempre debe tener firma
        Usuario suscriptor = existente != null ? existente.getMedico() : resolverSuscriptor(principal, cita);
        informe.setMedico(suscriptor);

        List<String> errores = validar(informe);
        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            if (existente != null) {
                informe.setIdInforme(existente.getIdInforme());
                informe.setFechaEmision(existente.getFechaEmision());
            }
            prepararFormulario(model, informe);
            return "informes/formulario";
        }

        if (existente != null) {
            existente.setTipo(informe.getTipo());
            existente.setLugar(informe.getLugar());
            existente.setDestinatario(informe.getDestinatario());
            existente.setAsunto(informe.getAsunto().trim());
            existente.setContenido(informe.getContenido().trim());
            existente.setCita(cita);
            informeRepo.save(existente);
            redirectAttributes.addFlashAttribute("mensajeExito", "Informe actualizado correctamente.");
            return "redirect:/informes/imprimir/" + existente.getIdInforme();
        }

        informe.setAsunto(informe.getAsunto().trim());
        informe.setContenido(informe.getContenido().trim());
        informe.setFechaEmision(LocalDate.now());

        InformeOdontologico guardado = informeRepo.save(informe);
        redirectAttributes.addFlashAttribute("mensajeExito", "Informe guardado. Ya puedes imprimirlo en la hoja membretada.");
        return "redirect:/informes/imprimir/" + guardado.getIdInforme();
    }

    // ===================== IMPRESIÓN =====================

    @GetMapping("/imprimir/{id}")
    public String imprimir(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        InformeOdontologico informe = informeRepo.findById(id).orElse(null);
        if (informe == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El informe solicitado no existe.");
            return "redirect:/informes";
        }

        model.addAttribute("informe", informe);
        model.addAttribute("fechaLarga", informe.getFechaEmision() != null
                ? informe.getFechaEmision().format(FORMATO_FECHA_LARGA)
                : LocalDate.now().format(FORMATO_FECHA_LARGA));

        Paciente paciente = informe.getPaciente();
        if (paciente != null && paciente.getFechaNacimiento() != null) {
            Period edad = Period.between(paciente.getFechaNacimiento(), informe.getFechaEmision() != null
                    ? informe.getFechaEmision() : LocalDate.now());
            model.addAttribute("edadCalculada", edad.getYears() + " años");
        }

        // Firma digital registrada en la última atención odontológica del paciente
        String firma = null;
        if (paciente != null) {
            firma = expOdontoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(paciente.getIdPaciente())
                    .stream()
                    .findFirst()
                    .map(ExpedienteOdontopediatrico::getFirmaMedicoBase64)
                    .filter(f -> f != null && f.startsWith("data:image"))
                    .orElse(null);
        }
        model.addAttribute("firmaDigital", firma);

        return "informes/informe_print";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (!informeRepo.existsById(id)) {
            redirectAttributes.addFlashAttribute("mensajeError", "El informe solicitado no existe.");
            return "redirect:/informes";
        }
        informeRepo.deleteById(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Informe eliminado del historial.");
        return "redirect:/informes";
    }

    // ===================== APOYO =====================

    private void prepararFormulario(Model model, InformeOdontologico informe) {
        model.addAttribute("informe", informe);
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("tiposInforme", InformeOdontologico.TIPOS);
        model.addAttribute("sedes", SEDES);
        model.addAttribute("citasPaciente", informe.getPaciente() != null
                ? citasDelPaciente(informe.getPaciente().getIdPaciente())
                : List.of());
    }

    private List<Cita> citasDelPaciente(Long idPaciente) {
        List<Cita> citas = new ArrayList<>(citaRepo.findByPacienteIdPaciente(idPaciente));
        citas.sort(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHora).reversed());
        return citas;
    }

    /**
     * Profesional que firma el documento: el de la consulta de referencia, el usuario
     * en sesión si es médico, o la odontóloga titular cuando el informe lo redacta recepción.
     */
    private Usuario resolverSuscriptor(Principal principal, Cita cita) {
        if (cita != null && cita.getMedico() != null) {
            return cita.getMedico();
        }

        Usuario actual = principal != null
                ? usuarioService.buscarPorUsername(principal.getName()).orElse(null)
                : null;

        if (actual != null && ("ODONTOPEDIATRA".equals(actual.getRol())
                || "ODONTOLOGIA".equals(actual.getRol())
                || "PEDIATRA".equals(actual.getRol()))) {
            return actual;
        }

        Usuario titular = usuarioRepo.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> "ODONTOPEDIATRA".equals(u.getRol()) || "ODONTOLOGIA".equals(u.getRol()))
                .findFirst()
                .orElse(null);

        return titular != null ? titular : actual;
    }

    private List<String> validar(InformeOdontologico informe) {
        List<String> errores = new ArrayList<>();

        String asunto = informe.getAsunto() != null ? informe.getAsunto().trim() : "";
        if (!ValidacionesClinicas.textoClinicoValido(asunto)) {
            errores.add("El asunto debe ser una frase real (mínimo 10 caracteres y 2 palabras), sin letras repetidas ni cadenas al azar.");
        }

        String contenido = informe.getContenido() != null ? informe.getContenido().trim() : "";
        if (!ValidacionesClinicas.recomendacionValida(contenido)) {
            errores.add("El cuerpo del informe debe ser texto descriptivo: mínimo 15 caracteres y 3 palabras coherentes.");
        }

        if (informe.getMedico() == null) {
            errores.add("No se pudo determinar el profesional que suscribe el informe.");
        }

        return errores;
    }
}
