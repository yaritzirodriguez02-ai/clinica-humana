package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.FotoPaciente;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.repository.FotoPacienteRepository;
import com.clinicahumana.clinica_humana.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;

/**
 * Fotos clínicas del paciente (placas, radiografías, estudios y retrato).
 * La foto marcada como perfil se muestra en el expediente y en el directorio.
 */
@Controller
@RequestMapping("/pacientes/fotos")
public class FotoPacienteController {

    /** Tamaño máximo por archivo (se valida además en application.properties). */
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    @Autowired
    private FotoPacienteRepository fotoRepository;

    @Autowired
    private PacienteService pacienteService;

    @PostMapping("/{idPaciente}/subir")
    public String subirFoto(@PathVariable("idPaciente") Long idPaciente,
                            @RequestParam("archivo") MultipartFile archivo,
                            @RequestParam(name = "descripcion", required = false) String descripcion,
                            @RequestParam(name = "perfil", required = false) String perfil,
                            RedirectAttributes redirectAttributes) {

        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        String volver = "redirect:/pacientes/expediente/" + idPaciente + "#fotos";

        if (archivo == null || archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Selecciona una imagen para subirla.");
            return volver;
        }

        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase() : "";
        if (!tipo.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El archivo debe ser una imagen (JPG, PNG, WEBP o similar).");
            return volver;
        }
        if (archivo.getSize() > MAX_BYTES) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "La imagen supera el límite de 8 MB. Redúcela e inténtalo de nuevo.");
            return volver;
        }

        try {
            String dataUri = "data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(archivo.getBytes());

            FotoPaciente foto = new FotoPaciente();
            foto.setPaciente(paciente);
            foto.setDatosBase64(dataUri);
            foto.setTipoContenido(tipo);
            foto.setNombreArchivo(archivo.getOriginalFilename());
            foto.setDescripcion(descripcion != null && !descripcion.isBlank()
                    ? descripcion.trim() : "Foto clínica");
            foto.setPerfil(esPrimeraFoto(idPaciente) || "S".equalsIgnoreCase(perfil));

            fotoRepository.save(foto);

            if (Boolean.TRUE.equals(foto.getPerfil())) {
                marcarComoPerfil(foto);
            }

            redirectAttributes.addFlashAttribute("mensajeExito", "Foto agregada al expediente del paciente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar la imagen: " + e.getMessage());
        }

        return volver;
    }

    /** Deja una sola foto como retrato del paciente. */
    @PostMapping("/{idFoto}/perfil")
    @Transactional
    public String marcarPerfil(@PathVariable("idFoto") Long idFoto, RedirectAttributes redirectAttributes) {
        FotoPaciente foto = fotoRepository.findById(idFoto).orElse(null);
        if (foto == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La foto ya no existe.");
            return "redirect:/pacientes";
        }

        marcarComoPerfil(foto);
        redirectAttributes.addFlashAttribute("mensajeExito", "Foto de perfil actualizada.");
        return "redirect:/pacientes/expediente/" + foto.getPaciente().getIdPaciente() + "#fotos";
    }

    @PostMapping("/{idFoto}/eliminar")
    @Transactional
    public String eliminarFoto(@PathVariable("idFoto") Long idFoto, RedirectAttributes redirectAttributes) {
        FotoPaciente foto = fotoRepository.findById(idFoto).orElse(null);
        if (foto == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La foto ya no existe.");
            return "redirect:/pacientes";
        }

        Long idPaciente = foto.getPaciente().getIdPaciente();
        fotoRepository.delete(foto);

        // Si era la foto de perfil, se toma la más reciente que quede.
        if (Boolean.TRUE.equals(foto.getPerfil()) && !fotoRepository
                .findByPacienteIdPacienteOrderByFechaSubidaDesc(idPaciente).isEmpty()) {
            FotoPaciente nueva = fotoRepository
                    .findByPacienteIdPacienteOrderByFechaSubidaDesc(idPaciente).get(0);
            nueva.setPerfil(Boolean.TRUE);
            fotoRepository.save(nueva);
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Foto eliminada del expediente.");
        return "redirect:/pacientes/expediente/" + idPaciente + "#fotos";
    }

    private boolean esPrimeraFoto(Long idPaciente) {
        return fotoRepository.findByPacienteIdPacienteOrderByFechaSubidaDesc(idPaciente).isEmpty();
    }

    private void marcarComoPerfil(FotoPaciente foto) {
        Long idPaciente = foto.getPaciente().getIdPaciente();
        List<FotoPaciente> fotos = fotoRepository.findByPacienteIdPacienteOrderByFechaSubidaDesc(idPaciente);
        for (FotoPaciente otra : fotos) {
            boolean debeSerPerfil = otra.getIdFoto().equals(foto.getIdFoto());
            if (!Boolean.valueOf(debeSerPerfil).equals(otra.getPerfil())) {
                otra.setPerfil(debeSerPerfil);
                fotoRepository.save(otra);
            }
        }
    }
}
