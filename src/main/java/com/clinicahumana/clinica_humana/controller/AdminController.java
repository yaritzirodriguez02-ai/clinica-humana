package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.EmpleadoRepository;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EmpleadoRepository empleadoRepo;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private PagoRepository pagoRepo;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalEmpleados = empleadoRepo.count();
        long totalPacientes = pacienteRepo.count();
        long citasHoy = citaRepo.findAll().stream()
                .filter(c -> c.getFecha() != null && c.getFecha().isEqual(LocalDate.now()))
                .count();

        List<Pago> pagosHoy = pagoRepo.findAll().stream()
                .filter(p -> p.getFechaPago() != null && p.getFechaPago().toLocalDate().isEqual(LocalDate.now()))
                .toList();

        BigDecimal ingresosHoy = pagosHoy.stream()
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalEmpleados", totalEmpleados);
        model.addAttribute("totalPacientes", totalPacientes);
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("ingresosHoy", ingresosHoy);
        model.addAttribute("empleados", empleadoRepo.findAll());

        return "admin/dashboard";
    }
}