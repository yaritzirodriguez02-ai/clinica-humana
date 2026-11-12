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

/**
 * Imágenes del paciente, en dos apartados independientes:
 * <ul>
 *   <li>Foto de perfil: un único retrato que identifica al paciente en el expediente y el directorio.</li>
 *   <li>Galería clínica: placas, radiografías y fotografías de la atención dental (nunca se usa como perfil).</li>
 * </ul>
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

    /** Agrega una imagen a la galería clínica dental del paciente. */
    @PostMapping("/{idPaciente}/subir")
    public String subirFotoClinica(@PathVariable("idPaciente") Long idPaciente,
                                   @RequestParam("archivo") MultipartFile archivo,
                                   @RequestParam(name = "descripcion", required = false) String descripcion,
                                   RedirectAttributes redirectAttributes) {

        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        String volver = "redirect:/pacientes/expediente/" + idPaciente + "#fotos-clinicas";

        String error = validarImagen(archivo);
        if (error != null) {
            redirectAttributes.addFlashAttribute("mensajeError", error);
            return volver;
        }

        try {
            FotoPaciente foto = construirFoto(paciente, archivo);
            foto.setDescripcion(descripcion != null && !descripcion.isBlank()
                    ? descripcion.trim() : "Foto clínica");
            foto.setPerfil(Boolean.FALSE);
            fotoRepository.save(foto);

            redirectAttributes.addFlashAttribute("mensajeExito", "Foto agregada a la galería clínica del paciente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar la imagen: " + e.getMessage());
        }

        return volver;
    }

    /** Sube o reemplaza la foto de perfil (un solo retrato por paciente). */
    @PostMapping("/{idPaciente}/perfil/subir")
    @Transactional
    public String subirFotoPerfil(@PathVariable("idPaciente") Long idPaciente,
                                  @RequestParam("archivo") MultipartFile archivo,
                                  RedirectAttributes redirectAttributes) {

        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        String volver = "redirect:/pacientes/expediente/" + idPaciente + "#foto-perfil";

        String error = validarImagen(archivo);
        if (error != null) {
            redirectAttributes.addFlashAttribute("mensajeError", error);
            return volver;
        }

        try {
            fotoRepository.deleteAll(fotoRepository.findByPacienteIdPacienteAndPerfilTrue(idPaciente));

            FotoPaciente foto = construirFoto(paciente, archivo);
            foto.setDescripcion("Foto de perfil");
            foto.setPerfil(Boolean.TRUE);
            fotoRepository.save(foto);

            redirectAttributes.addFlashAttribute("mensajeExito", "Foto de perfil actualizada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar la foto de perfil: " + e.getMessage());
        }

        return volver;
    }

    @PostMapping("/{idFoto}/eliminar")
    @Transactional
    public String eliminarFoto(@PathVariable("idFoto") Long idFoto, RedirectAttributes redirectAttributes) {
        FotoPaciente foto = fotoRepository.findById(idFoto).orElse(null);
        if (foto == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La imagen ya no existe.");
            return "redirect:/pacientes";
        }

        Long idPaciente = foto.getPaciente().getIdPaciente();
        boolean eraPerfil = Boolean.TRUE.equals(foto.getPerfil());
        fotoRepository.delete(foto);

        redirectAttributes.addFlashAttribute("mensajeExito",
                eraPerfil ? "Foto de perfil eliminada." : "Imagen eliminada de la galería clínica.");
        return "redirect:/pacientes/expediente/" + idPaciente + (eraPerfil ? "#foto-perfil" : "#fotos-clinicas");
    }

    private String validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return "Selecciona una imagen para subirla.";
        }
        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase() : "";
        if (!tipo.startsWith("image/")) {
            return "El archivo debe ser una imagen (JPG, PNG, WEBP o similar).";
        }
        if (archivo.getSize() > MAX_BYTES) {
            return "La imagen supera el límite de 8 MB. Redúcela e inténtalo de nuevo.";
        }
        return null;
    }

    private FotoPaciente construirFoto(Paciente paciente, MultipartFile archivo) throws Exception {
        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase() : "";

        FotoPaciente foto = new FotoPaciente();
        foto.setPaciente(paciente);
        foto.setDatosBase64("data:" + tipo + ";base64," + Base64.getEncoder().encodeToString(archivo.getBytes()));
        foto.setTipoContenido(tipo);
        foto.setNombreArchivo(archivo.getOriginalFilename());
        return foto;
    }
}
