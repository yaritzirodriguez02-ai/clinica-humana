package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Empleado;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.EmpleadoRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoRepository empleadoRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Listado de personal clínico y administrativo
    @GetMapping
    public String listarEmpleados(Model model) {
        List<Empleado> empleados = empleadoRepo.findAll();
        model.addAttribute("empleados", empleados);
        return "empleados/lista_empleados";
    }

    // Formulario para registrar nuevo empleado con credenciales
    @GetMapping("/nuevo")
    public String formularioNuevoEmpleado(Model model) {
        model.addAttribute("empleado", new Empleado());
        return "empleados/formulario_empleado";
    }

    // Guardar nuevo empleado y crear su usuario en el sistema
    @PostMapping("/guardar")
    public String guardarEmpleado(@Valid @ModelAttribute("empleado") Empleado empleado,
                                  BindingResult result,
                                  @RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "empleados/formulario_empleado";
        }

        try {
            // 1. Crear la cuenta de acceso vinculada
            Usuario usuario = new Usuario();
            usuario.setUsername(username.trim().toLowerCase());
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setNombreCompleto(empleado.getNombre() + " " + empleado.getApellidos());
            usuario.setEmail(empleado.getEmail());
            usuario.setRol(empleado.getCargo());
            usuario.setActivo(true);
            usuario = usuarioRepo.save(usuario);

            // 2. Asociar y guardar el perfil de empleado
            empleado.setUsuario(usuario);
            empleadoRepo.save(empleado);

            redirectAttributes.addFlashAttribute("mensajeExito", "Empleado registrado y cuenta creada exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al registrar el personal: " + e.getMessage());
            return "redirect:/empleados/nuevo";
        }

        return "redirect:/empleados";
    }
}