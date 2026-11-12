package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.model.FotoPaciente;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.FotoPacienteRepository;
import com.clinicahumana.clinica_humana.service.ClinicoYPagosService;
import com.clinicahumana.clinica_humana.service.PacienteService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private ClinicoYPagosService clinicoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private FotoPacienteRepository fotoPacienteRepository;

    // Listar los pacientes del área del usuario con opción de búsqueda (RF02)
    @GetMapping
    public String listarPacientes(@RequestParam(name = "buscar", required = false) String buscar,
                                  @RequestParam(name = "area", required = false) String slug,
                                  Principal principal,
                                  Model model) {
        String rol = rolActual(principal);
        boolean esAdmin = esAdmin(rol);
        AreaClinica areaRol = AreaClinica.paraRol(rol);

        // Cada panel ve sólo su área; el administrador alterna entre las dos con pestañas
        List<AreaClinica> areasPermitidas = esAdmin
                ? List.of(AreaClinica.values())
                : (areaRol != null ? List.of(areaRol) : List.of());

        AreaClinica area = esAdmin ? AreaClinica.desdeSlug(slug) : areaRol;
        if (area == null) {
            area = areasPermitidas.isEmpty() ? AreaClinica.ODONTOLOGIA : areasPermitidas.get(0);
        }
        if (!areasPermitidas.isEmpty() && !areasPermitidas.contains(area)) {
            area = areasPermitidas.get(0);
        }

        List<Paciente> lista = (buscar != null && !buscar.trim().isEmpty())
                ? pacienteService.buscarPorNombreOApellido(buscar.trim(), area)
                : pacienteService.listarPorArea(area);

        // Foto de perfil de cada paciente (si tiene) para mostrarla en el directorio
        Map<Long, FotoPaciente> fotosPerfil = new HashMap<>();
        if (!lista.isEmpty()) {
            fotoPacienteRepository.findByPacienteIdPacienteInAndPerfilTrue(
                            lista.stream().map(Paciente::getIdPaciente).toList())
                    .forEach(f -> fotosPerfil.putIfAbsent(f.getPaciente().getIdPaciente(), f));
        }

        model.addAttribute("pacientes", lista);
        model.addAttribute("fotosPerfil", fotosPerfil);
        model.addAttribute("buscar", buscar);
        model.addAttribute("areaActual", area);
        model.addAttribute("areasPermitidas", areasPermitidas);
        model.addAttribute("esAdmin", esAdmin);
        return "pacientes/lista_pacientes";
    }

    // Historial clínico completo del paciente (pediátrico + odontopediátrico) ordenado cronológicamente
    @GetMapping("/expediente/{idPaciente}")
    public String verExpedientePaciente(@PathVariable("idPaciente") Long idPaciente,
                                        Principal principal,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!pacienteService.buscarPorId(idPaciente).isPresent()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElseThrow();
        model.addAttribute("paciente", paciente);

        Usuario usuarioActual = usuarioService.buscarPorUsername(principal != null ? principal.getName() : "").orElse(null);
        String rol = usuarioActual != null ? usuarioActual.getRol() : "";
        boolean esAdmin = esAdmin(rol);

        // Un paciente sólo puede abrirse desde el panel del área donde fue registrado
        AreaClinica areaRol = AreaClinica.paraRol(rol);
        if (!esAdmin && areaRol != null && paciente.getArea() != null && paciente.getArea() != areaRol) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El paciente " + paciente.getNombre() + " " + paciente.getApellidos()
                            + " pertenece al área de " + paciente.getArea().getEtiqueta() + ".");
            return "redirect:/pacientes";
        }

        boolean esPediatra = "PEDIATRA".equals(rol);
        boolean esOdontopediatra = "ODONTOPEDIATRA".equals(rol);
        boolean esRecepcionista = "RECEPCIONISTA".equals(rol);

        model.addAttribute("rolUsuario", rol);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esPediatra", esPediatra);
        model.addAttribute("esOdontopediatra", esOdontopediatra);
        model.addAttribute("esRecepcionista", esRecepcionista);
        model.addAttribute("idPaciente", idPaciente);

        List<ExpedientePediatrico> historiaPediatrica;
        List<ExpedienteOdontopediatrico> historiaOdontopediatrica;

        if (esPediatra) {
            historiaPediatrica = clinicoService.obtenerHistorialPediatricoPorMedico(idPaciente, usuarioActual.getIdUsuario());
            historiaOdontopediatrica = List.of();
        } else if (esOdontopediatra || esRecepcionista) {
            historiaPediatrica = List.of();
            historiaOdontopediatrica = clinicoService.obtenerHistorialOdontopediatricoPorMedico(idPaciente, usuarioActual.getIdUsuario());
        } else {
            historiaPediatrica = clinicoService.obtenerHistorialPediatrico(idPaciente);
            historiaOdontopediatrica = clinicoService.obtenerHistorialOdontopediatrico(idPaciente);
        }

        List<ExpedientePediatrico> pediatraCronologica = new ArrayList<>(historiaPediatrica);
        Collections.reverse(pediatraCronologica);
        List<ExpedienteOdontopediatrico> odontoCronologica = new ArrayList<>(historiaOdontopediatrica);
        Collections.reverse(odontoCronologica);

        // Retrato de perfil y galería clínica (son apartados distintos)
        FotoPaciente fotoPerfil = fotoPacienteRepository
                .findFirstByPacienteIdPacienteAndPerfilTrue(idPaciente)
                .orElse(null);
        List<FotoPaciente> fotos = fotoPacienteRepository
                .findByPacienteIdPacienteAndPerfilFalseOrderByFechaSubidaDesc(idPaciente);
        model.addAttribute("fotos", fotos);
        model.addAttribute("fotoPerfil", fotoPerfil);

        model.addAttribute("historiaPediatrica", historiaPediatrica);
        model.addAttribute("historiaOdontopediatrica", historiaOdontopediatrica);
        model.addAttribute("historiaPediatricaCronologica", pediatraCronologica);
        model.addAttribute("historiaOdontopediatricaCronologica", odontoCronologica);

        return "pacientes/ver_expediente";
    }

    // Mostrar formulario de nuevo paciente (RF01, RF03)
    @GetMapping("/nuevo")
    public String formularioNuevoPaciente(@RequestParam(name = "area", required = false) String slug,
                                          Principal principal,
                                          Model model) {
        String rol = rolActual(principal);
        AreaClinica area = areaDeAlta(rol, slug);

        Paciente paciente = new Paciente();
        paciente.setArea(area);
        paciente.setTutor(new Tutor()); // Instanciar para el formulario anidado
        paciente.getTutor().setArea(area);

        prepararFormulario(model, paciente, area, esAdmin(rol));
        return "pacientes/formulario";
    }

    // Guardar nuevo paciente o editar existente
    @PostMapping("/guardar")
    public String guardarPaciente(@Valid @ModelAttribute("paciente") Paciente paciente,
                                 BindingResult result,
                                 @RequestParam(name = "tieneAlergias", required = false) String tieneAlergias,
                                 @RequestParam(name = "tieneAntecedentes", required = false) String tieneAntecedentes,
                                 @RequestParam(name = "areaSlug", required = false) String slug,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        // El área se fija en el servidor: el rol la impone y el administrador crea desde su pestaña
        String rol = rolActual(principal);
        AreaClinica area = areaDeAlta(rol, slug);
        // Condicionales Sí/No del formulario: si la respuesta es "No", se limpia el detalle
        if (!"S".equals(tieneAlergias)) {
            paciente.setAlergias("");
        } else if (paciente.getAlergias() != null) {
            paciente.setAlergias(paciente.getAlergias().trim());
        }

        if (!"S".equals(tieneAntecedentes)) {
            paciente.setAntecedentes("");
        } else if (paciente.getAntecedentes() != null) {
            paciente.setAntecedentes(paciente.getAntecedentes().trim());
        }

        paciente.setArea(area);

        // En odontología la atención es de libre edad: el tutor es opcional, así que
        // se descarta cuando el campo se dejó vacío en lugar de exigirlo.
        if (paciente.getTutor() != null) {
            boolean nombreVacio = paciente.getTutor().getNombre() == null
                    || paciente.getTutor().getNombre().isBlank();
            boolean apellidosVacios = paciente.getTutor().getApellidos() == null
                    || paciente.getTutor().getApellidos().isBlank();
            if (nombreVacio && apellidosVacios) {
                paciente.setTutor(null);
            }
        }

        if (result.hasErrors()) {
            prepararFormulario(model, paciente, area, esAdmin(rol));
            return "pacientes/formulario";
        }

        try {
            pacienteService.registrarOActualizarPaciente(paciente);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Paciente y tutor guardados exitosamente en "
                    + area.getEtiqueta() + "!");
            return "redirect:/pacientes?area=" + area.getSlug();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorCurp", e.getMessage());
            prepararFormulario(model, paciente, area, esAdmin(rol));
            return "pacientes/formulario";
        }
    }

    // Editar paciente existente
    @GetMapping("/editar/{id}")
    public String editarPaciente(@PathVariable("id") Long id,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String rol = rolActual(principal);
        boolean esAdmin = esAdmin(rol);
        AreaClinica areaRol = AreaClinica.paraRol(rol);

        return pacienteService.buscarPorId(id).map(paciente -> {
            AreaClinica area = paciente.getArea() != null
                    ? paciente.getArea()
                    : (areaRol != null ? areaRol : AreaClinica.ODONTOLOGIA);

            if (!esAdmin && areaRol != null && area != areaRol) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El paciente pertenece al área de " + area.getEtiqueta() + ".");
                return "redirect:/pacientes";
            }

            if (paciente.getTutor() == null) {
                paciente.setTutor(new Tutor());
            }
            prepararFormulario(model, paciente, area, esAdmin);
            return "pacientes/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente con ID " + id + " no existe.");
            return "redirect:/pacientes";
        });
    }

    // ===================== APOYO =====================

    private void prepararFormulario(Model model, Paciente paciente, AreaClinica area, boolean esAdmin) {
        model.addAttribute("paciente", paciente);
        model.addAttribute("areaActual", area);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("tutoresExistentes", pacienteService.listarTutoresPorArea(area));
    }

    /** Área de alta: el rol la impone; el administrador la elige desde la pestaña activa. */
    private AreaClinica areaDeAlta(String rol, String slug) {
        AreaClinica areaRol = AreaClinica.paraRol(rol);
        if (areaRol != null) {
            return areaRol;
        }
        AreaClinica elegida = AreaClinica.desdeSlug(slug);
        return elegida != null ? elegida : AreaClinica.ODONTOLOGIA;
    }

    private boolean esAdmin(String rol) {
        return "ADMIN".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);
    }

    private String rolActual(Principal principal) {
        if (principal == null) {
            return "";
        }
        Usuario usuario = usuarioService.buscarPorUsername(principal.getName()).orElse(null);
        return usuario != null && usuario.getRol() != null ? usuario.getRol().toUpperCase() : "";
    }
}