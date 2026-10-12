package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Empleado;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.EmpleadoRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

/**
 * CRUD completo de Personal/Colaboradores (CREAR, LISTAR, EDITAR, ELIMINAR/DAR DE BAJA).
 * Todas las rutas (/empleados/**) están reservadas EXCLUSIVAMENTE al rol ADMIN en SecurityConfig.
 */
@Controller
@RequestMapping("/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoRepository empleadoRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // READ: listado de todo el personal ACTIVO
    @GetMapping
    public String listarEmpleados(Model model) {
        List<Empleado> empleados = empleadoRepo.findAll().stream()
                .filter(e -> e.getUsuario() == null || !Boolean.FALSE.equals(e.getUsuario().getActivo()))
                .sorted(Comparator.comparing(Empleado::getApellidos, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("empleados", empleados);
        return "empleados/lista_empleados";
    }

    // CREATE: formulario de alta
    @GetMapping("/nuevo")
    public String formularioNuevoEmpleado(Model model) {
        model.addAttribute("empleado", new Empleado());
        model.addAttribute("esEdicion", false);
        return "empleados/formulario_empleado";
    }

    // UPDATE: formulario de edición con los datos precargados
    @GetMapping("/editar/{id}")
    public String formularioEditarEmpleado(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Empleado empleado = empleadoRepo.findById(id).orElse(null);
        if (empleado == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El colaborador solicitado no existe.");
            return "redirect:/empleados";
        }
        model.addAttribute("empleado", empleado);
        model.addAttribute("esEdicion", true);
        return "empleados/formulario_empleado";
    }

    // CREATE / UPDATE: alta (id vacío) o actualización (id presente) con contraseña encriptada en BCrypt
    @PostMapping("/guardar")
    public String guardarEmpleado(@Valid @ModelAttribute("empleado") Empleado empleado,
                                  BindingResult result,
                                  @RequestParam("username") String username,
                                  @RequestParam(value = "password", required = false) String password,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        boolean esEdicion = empleado.getIdEmpleado() != null;
        model.addAttribute("esEdicion", esEdicion);

        if (result.hasErrors()) {
            return "empleados/formulario_empleado";
        }

        String usernameNormalizado = username == null ? "" : username.trim().toLowerCase();
        if (usernameNormalizado.isEmpty()) {
            model.addAttribute("errorFormulario", "El usuario (username) es obligatorio.");
            return "empleados/formulario_empleado";
        }

        boolean passwordVacio = password == null || password.trim().isEmpty();
        if (!esEdicion && passwordVacio) {
            model.addAttribute("errorFormulario", "La contraseña es obligatoria para crear la cuenta de acceso.");
            return "empleados/formulario_empleado";
        }
        if (!passwordVacio && password.trim().length() < 4) {
            model.addAttribute("errorFormulario", "La contraseña debe tener al menos 4 caracteres.");
            return "empleados/formulario_empleado";
        }
        if (empleado.getEmail() == null || empleado.getEmail().trim().isEmpty()) {
            model.addAttribute("errorFormulario", "El correo electrónico es obligatorio.");
            return "empleados/formulario_empleado";
        }

        try {
            if (!esEdicion) {
                // ---------- CREAR (contraseña encriptada en BCrypt) ----------
                if (usuarioRepo.existsByUsername(usernameNormalizado)) {
                    model.addAttribute("errorFormulario", "El usuario '" + usernameNormalizado + "' ya existe. Elija otro username.");
                    return "empleados/formulario_empleado";
                }

                Usuario usuario = new Usuario();
                usuario.setUsername(usernameNormalizado);
                usuario.setPassword(passwordEncoder.encode(password.trim()));
                usuario.setNombreCompleto(empleado.getNombre().trim() + " " + empleado.getApellidos().trim());
                usuario.setEmail(empleado.getEmail().trim());
                usuario.setRol(empleado.getCargo());
                usuario.setActivo(true);
                usuario = usuarioRepo.save(usuario);

                empleado.setUsuario(usuario);
                empleadoRepo.save(empleado);

                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Empleado registrado y cuenta creada exitosamente con contraseña encriptada (BCrypt).");
            } else {
                // ---------- ACTUALIZAR ----------
                Empleado existente = empleadoRepo.findById(empleado.getIdEmpleado()).orElseThrow();
                existente.setNombre(empleado.getNombre().trim());
                existente.setApellidos(empleado.getApellidos().trim());
                existente.setCargo(empleado.getCargo());
                existente.setCedulaProfesional(empleado.getCedulaProfesional());
                existente.setTelefono(empleado.getTelefono());
                existente.setEmail(empleado.getEmail().trim());

                Usuario usuario = existente.getUsuario();
                if (usuario == null) {
                    usuario = new Usuario();
                    usuario.setUsername(usernameNormalizado);
                    usuario.setPassword(passwordEncoder.encode(password.trim()));
                    usuario.setActivo(true);
                } else {
                    if (!usuario.getUsername().equals(usernameNormalizado) && usuarioRepo.existsByUsername(usernameNormalizado)) {
                        model.addAttribute("errorFormulario", "El usuario '" + usernameNormalizado + "' ya existe. Elija otro username.");
                        return "empleados/formulario_empleado";
                    }
                    usuario.setUsername(usernameNormalizado);
                    if (!passwordVacio) {
                        usuario.setPassword(passwordEncoder.encode(password.trim()));
                    }
                }
                usuario.setNombreCompleto(existente.getNombre() + " " + existente.getApellidos());
                usuario.setEmail(existente.getEmail());
                usuario.setRol(existente.getCargo());
                usuarioRepo.save(usuario);

                existente.setUsuario(usuario);
                empleadoRepo.save(existente);

                redirectAttributes.addFlashAttribute("mensajeExito", "Colaborador actualizado correctamente.");
            }
        } catch (Exception e) {
            model.addAttribute("errorFormulario", "Error al guardar el personal: " + e.getMessage());
            return "empleados/formulario_empleado";
        }

        return "redirect:/empleados";
    }

    // DELETE / DAR DE BAJA (exclusivo ADMIN)
    @PostMapping("/eliminar/{id}")
    public String eliminarEmpleado(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Empleado empleado = empleadoRepo.findById(id).orElse(null);
        if (empleado == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El colaborador solicitado no existe.");
            return "redirect:/empleados";
        }

        Usuario usuario = empleado.getUsuario();

        // Si es médico con citas/historial clínico: baja lógica (cuenta inactiva, no puede iniciar sesión)
        boolean tieneHistorial = usuario != null && citaRepo.existsByMedicoIdUsuario(usuario.getIdUsuario());
        if (tieneHistorial) {
            usuario.setActivo(false);
            usuarioRepo.save(usuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Colaborador dado de baja: la cuenta quedó INACTIVA y ya no podrá iniciar sesión (conserva su historial clínico).");
            return "redirect:/empleados";
        }

        // En caso contrario: eliminación permanente
        empleadoRepo.delete(empleado);
        if (usuario != null) {
            try {
                usuarioRepo.delete(usuario);
                redirectAttributes.addFlashAttribute("mensajeExito", "Colaborador eliminado permanentemente del sistema.");
            } catch (DataIntegrityViolationException e) {
                usuario.setActivo(false);
                usuarioRepo.save(usuario);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "No fue posible borrar el registro por información clínica asociada; la cuenta se dio de baja (INACTIVA).");
            }
        } else {
            redirectAttributes.addFlashAttribute("mensajeExito", "Colaborador eliminado permanentemente del sistema.");
        }
        return "redirect:/empleados";
    }
}