package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.service.DepuracionService;
import com.clinicahumana.clinica_humana.service.DepuracionService.Area;
import com.clinicahumana.clinica_humana.service.DepuracionService.ResumenArea;
import com.clinicahumana.clinica_humana.service.DepuracionService.ResultadoDepuracion;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Panel de limpieza de registros.
 *
 * Cada área clínica ve y depura únicamente lo suyo: odontología y recepción
 * trabajan juntas sobre el área dental, pediatría es autónoma y el administrador
 * conserva el control de las dos áreas.
 */
@Controller
@RequestMapping("/registros")
public class DepuracionController {

    @Autowired
    private DepuracionService depuracionService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String panel(@RequestParam(name = "area", required = false) String slug,
                        Principal principal,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        List<Area> permitidas = areasPermitidas(principal);
        if (permitidas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Area area = Area.desdeSlug(slug);
        if (area == null) {
            area = permitidas.get(0);
        }
        if (!permitidas.contains(area)) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No tienes permiso para administrar los registros de esa área.");
            return "redirect:/registros?area=" + permitidas.get(0).getSlug();
        }

        ResumenArea resumen = depuracionService.resumen(area);
        model.addAttribute("resumen", resumen);
        model.addAttribute("areaActual", area);
        model.addAttribute("areasPermitidas", permitidas);
        model.addAttribute("esAdmin", esAdmin(principal));
        model.addAttribute("esOdontologia", area == Area.ODONTOLOGIA);
        return "registros/limpieza";
    }

    // ===================== VACIADO DEL ÁREA =====================

    @PostMapping("/{area}/vaciar")
    public String vaciar(@PathVariable("area") String slug,
                         @RequestParam(name = "confirmacion", required = false) String confirmacion,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {

        Area area = areaAutorizada(slug, principal);

        if (confirmacion == null || !"VACIAR".equalsIgnoreCase(confirmacion.trim())) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Para vaciar el área escribe la palabra VACIAR en el campo de confirmación.");
            return "redirect:/registros?area=" + area.getSlug();
        }

        try {
            ResultadoDepuracion r = depuracionService.vaciarArea(area);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Área de " + area.getEtiqueta() + " vaciada: " + r.pacientes() + " paciente(s), "
                            + r.citas() + " cita(s), " + r.consultas() + " consulta(s), "
                            + r.recetas() + " receta(s), " + r.pagos() + " cobro(s), "
                            + r.informes() + " informe(s), " + r.carnets() + " carnet(s) y "
                            + r.tutores() + " tutor(es) eliminados.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se pudo vaciar el área: " + mensaje(e));
        }
        return "redirect:/registros?area=" + area.getSlug();
    }

    // ===================== BORRADO INDIVIDUAL =====================

    @PostMapping("/{area}/cita/{id}")
    public String eliminarCita(@PathVariable("area") String slug, @PathVariable("id") Long id,
                               Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarCita(area, id),
                "Cita eliminada junto con su consulta, receta y cobro asociados.");
    }

    @PostMapping("/{area}/consulta/{id}")
    public String eliminarConsulta(@PathVariable("area") String slug, @PathVariable("id") Long id,
                                   Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarConsulta(area, id),
                "Consulta eliminada del expediente.");
    }

    @PostMapping("/{area}/receta/{id}")
    public String eliminarReceta(@PathVariable("area") String slug, @PathVariable("id") Long id,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarReceta(area, id),
                "Receta eliminada.");
    }

    @PostMapping("/{area}/pago/{id}")
    public String eliminarPago(@PathVariable("area") String slug, @PathVariable("id") Long id,
                               Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarPago(area, id),
                "Cobro eliminado de la caja.");
    }

    @PostMapping("/{area}/informe/{id}")
    public String eliminarInforme(@PathVariable("area") String slug, @PathVariable("id") Long id,
                                  Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarInforme(area, id),
                "Informe membretado eliminado.");
    }

    @PostMapping("/{area}/carnet/{id}")
    public String eliminarCarnet(@PathVariable("area") String slug, @PathVariable("id") Long id,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarCarnet(area, id),
                "Carnet de citas eliminado.");
    }

    @PostMapping("/{area}/paciente/{id}")
    public String eliminarPaciente(@PathVariable("area") String slug, @PathVariable("id") Long id,
                                   Principal principal, RedirectAttributes redirectAttributes) {
        Area area = areaAutorizada(slug, principal);
        boolean admin = esAdmin(principal);
        return ejecutar(area, redirectAttributes,
                () -> depuracionService.eliminarPaciente(area, id, admin),
                "Paciente eliminado con todo su historial clínico.");
    }

    // ===================== APOYO =====================

    private String ejecutar(Area area, RedirectAttributes redirectAttributes,
                            Runnable accion, String mensajeExito) {
        try {
            accion.run();
            redirectAttributes.addFlashAttribute("mensajeExito", mensajeExito);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensajeError", mensaje(e));
        }
        return "redirect:/registros?area=" + area.getSlug();
    }

    private String mensaje(RuntimeException e) {
        return (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : "Ocurrió un error inesperado al borrar el registro.";
    }

    private Area areaAutorizada(String slug, Principal principal) {
        Area area = Area.desdeSlug(slug);
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!areasPermitidas(principal).contains(area)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return area;
    }

    private List<Area> areasPermitidas(Principal principal) {
        String rol = rolActual(principal);
        if ("ADMIN".equals(rol) || "ADMINISTRADOR".equals(rol)) {
            return List.of(Area.ODONTOLOGIA, Area.PEDIATRIA);
        }
        if ("PEDIATRA".equals(rol)) {
            return List.of(Area.PEDIATRIA);
        }
        if ("ODONTOPEDIATRA".equals(rol) || "ODONTOLOGIA".equals(rol) || "RECEPCIONISTA".equals(rol)) {
            return List.of(Area.ODONTOLOGIA);
        }
        return List.of();
    }

    private boolean esAdmin(Principal principal) {
        String rol = rolActual(principal);
        return "ADMIN".equals(rol) || "ADMINISTRADOR".equals(rol);
    }

    private String rolActual(Principal principal) {
        if (principal == null) {
            return "";
        }
        Usuario usuario = usuarioService.buscarPorUsername(principal.getName()).orElse(null);
        return usuario != null && usuario.getRol() != null ? usuario.getRol().toUpperCase() : "";
    }
}
