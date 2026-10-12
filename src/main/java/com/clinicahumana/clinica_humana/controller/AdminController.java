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
        LocalDate hoy = LocalDate.now();
        long totalEmpleados = empleadoRepo.count();
        long totalPacientes = pacienteRepo.count();
        long citasHoy = citaRepo.countByFecha(hoy);

        List<Pago> pagosHoy = pagoRepo.findByFechaPagoBetween(hoy.atStartOfDay(), hoy.atTime(23, 59, 59));

        BigDecimal ingresosHoy = pagosHoy.stream()
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalEmpleados", totalEmpleados);
        model.addAttribute("totalPacientes", totalPacientes);
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("ingresosHoy", ingresosHoy);
        model.addAttribute("empleados", empleadoRepo.findAll());

        // Auditoría de agenda: citas de hoy y próximas citas de la clínica
        model.addAttribute("fechaHoy", hoy);
        model.addAttribute("citasHoyLista", citaRepo.findByFechaOrderByHoraAsc(hoy));
        model.addAttribute("proximasCitas",
                citaRepo.findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(hoy).stream().limit(10).toList());

        return "admin/dashboard";
    }
}