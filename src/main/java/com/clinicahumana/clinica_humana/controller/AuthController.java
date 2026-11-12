package com.clinicahumana.clinica_humana.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Entrada al login desde la portada: la tarjeta elegida define el panel
     * (Pediatría u Odontología) y el login se pinta con el color de esa especialidad.
     */
    @GetMapping("/entrar/{panel}")
    public String entrar(@PathVariable("panel") String panel, Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/citas";
        }
        String area = "ODONTOLOGIA".equalsIgnoreCase(panel) ? "ODONTOLOGIA" : "PEDIATRIA";
        model.addAttribute("panel", area);
        return "login";
    }

    /**
     * Portada pública: la doctora elige su panel (Pediatría u Odontología)
     * antes de llegar al login. Si ya tiene sesión activa, va directo a su agenda.
     */
    @GetMapping("/")
    public String inicio(Principal principal) {
        if (principal != null) {
            return "redirect:/citas";
        }
        return "bienvenida";
    }
}