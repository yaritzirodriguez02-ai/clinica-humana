package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private TutorRepository tutorRepository;

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteRepository.findById(id);
    }

    public Optional<Paciente> buscarPorCurp(String curp) {
        return pacienteRepository.findByCurp(curp);
    }

    public List<Paciente> buscarPorNombreOApellido(String termino) {
        return pacienteRepository.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(termino, termino);
    }

    @Transactional
    public Paciente registrarOActualizarPaciente(Paciente paciente) {
        // Validar unicidad de CURP al crear nuevo paciente
        if (paciente.getIdPaciente() == null && pacienteRepository.existsByCurp(paciente.getCurp())) {
            throw new IllegalArgumentException("Ya existe un paciente registrado con la CURP: " + paciente.getCurp());
        }

        // Si el tutor no ha sido persistido, se guarda primero
        if (paciente.getTutor() != null && paciente.getTutor().getIdTutor() == null) {
            Tutor tutorGuardado = tutorRepository.save(paciente.getTutor());
            paciente.setTutor(tutorGuardado);
        }

        return pacienteRepository.save(paciente);
    }

    public List<Tutor> listarTutores() {
        return tutorRepository.findAll();
    }

    @Transactional
    public Tutor guardarTutor(Tutor tutor) {
        return tutorRepository.save(tutor);
    }
}