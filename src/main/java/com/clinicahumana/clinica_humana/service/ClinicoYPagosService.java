package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.ExpedientePediatricoRepository;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ClinicoYPagosService {

    @Autowired
    private ExpedientePediatricoRepository expPediatricoRepo;

    @Autowired
    private ExpedienteOdontopediatricoRepository expOdontoRepo;

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private CitaRepository citaRepo;

    // CU03: Guardar Consulta Pediátrica y marcar cita completada
    @Transactional
    public ExpedientePediatrico guardarConsultaPediatrica(ExpedientePediatrico exp) {
        Long idCita = exp.getCita().getIdCita();
        Cita citaExistente = citaRepo.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con ID: " + idCita));

        // Actualizar estado de la cita existente en BD sin violar validaciones @NotNull
        citaExistente.setEstado("COMPLETADA");
        citaRepo.save(citaExistente);

        // Asociar las referencias completas de la base de datos al expediente
        exp.setCita(citaExistente);
        exp.setPaciente(citaExistente.getPaciente());
        exp.setMedico(citaExistente.getMedico());

        return expPediatricoRepo.save(exp);
    }

    public List<ExpedientePediatrico> obtenerHistorialPediatrico(Long idPaciente) {
        return expPediatricoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(idPaciente);
    }

    // CU04: Guardar Consulta Dental Odontopediátrica y marcar cita completada
    @Transactional
    public ExpedienteOdontopediatrico guardarConsultaOdontopediatrica(ExpedienteOdontopediatrico exp) {
        Long idCita = exp.getCita().getIdCita();
        Cita citaExistente = citaRepo.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con ID: " + idCita));

        // Actualizar estado de la cita existente en BD sin violar validaciones @NotNull
        citaExistente.setEstado("COMPLETADA");
        citaRepo.save(citaExistente);

        // Asociar las referencias completas de la base de datos al expediente
        exp.setCita(citaExistente);
        exp.setPaciente(citaExistente.getPaciente());
        exp.setMedico(citaExistente.getMedico());

        return expOdontoRepo.save(exp);
    }

    public List<ExpedienteOdontopediatrico> obtenerHistorialOdontopediatrico(Long idPaciente) {
        return expOdontoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(idPaciente);
    }

    // CU06: Registro de Cobro y cálculo de IVA
    @Transactional
    public Pago registrarPago(Pago pago) {
        if (Boolean.TRUE.equals(pago.getAplicaIva())) {
            BigDecimal iva = pago.getMontoBase().multiply(new BigDecimal("0.16")).setScale(2, RoundingMode.HALF_UP);
            pago.setMontoIva(iva);
            pago.setMontoTotal(pago.getMontoBase().add(iva));
        } else {
            pago.setMontoIva(BigDecimal.ZERO);
            pago.setMontoTotal(pago.getMontoBase());
        }
        return pagoRepo.save(pago);
    }

    // CU07: Consultas para Reportes
    public List<Pago> obtenerPagosPorMedico(Long idMedico) {
        return pagoRepo.findByMedicoIdUsuario(idMedico);
    }
}