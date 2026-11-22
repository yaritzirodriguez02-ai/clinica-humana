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
     * Entrada al login desde la portada: cada panel tiene su propia ruta fuerte
     * para que Spring no pierda el segmento {panel} tras la redirección del
     * filtro de bienvenida (/) y el login se pinte con el color de esa especialidad.
     */
    @GetMapping("/entrar/ODONTOLOGIA")
    public String entrarOdontologia(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/citas";
        }
        model.addAttribute("panel", "ODONTOLOGIA");
        return "login";
    }

    @GetMapping("/entrar/PEDIATRIA")
    public String entrarPediatria(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/citas";
        }
        model.addAttribute("panel", "PEDIATRIA");
        return "login";
    }

    @GetMapping("/entrar/recepcion")
    public String entrarRecepcion(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/citas";
        }
        // Recepción usa el estilo neutro del sistema (no tiene color propio de área)
        return "login";
    }

    @GetMapping("/entrar/admin")
    public String entrarAdmin(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("panel", "ADMIN");
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