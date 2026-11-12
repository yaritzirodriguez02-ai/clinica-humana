package com.clinicahumana.clinica_humana.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

@Controller
public class AuthController {

    /**
     * La puerta de entrada del sistema es siempre la portada: si alguien escribe
     * /login (o Spring Security manda aquí por una ruta protegida) se le lleva a la
     * pantalla de bienvenida para que elija su panel. El formulario se muestra
     * únicamente en /entrar/{panel}.
     */
    @GetMapping("/login")
    public String login(Principal principal) {
        if (principal != null) {
            return "redirect:/citas";
        }
        return "redirect:/";
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
        // Pediatría y Odontología tienen su propio color; cualquier otra entrada
        // (recepción / administración) usa el estilo neutro del sistema.
        String area;
        if ("ODONTOLOGIA".equalsIgnoreCase(panel)) {
            area = "ODONTOLOGIA";
        } else if ("PEDIATRIA".equalsIgnoreCase(panel)) {
            area = "PEDIATRIA";
        } else {
            area = null;
        }
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