package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.AreaClinica;
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

    /** Directorio completo; sólo lo usa el administrador, que supervisa las dos áreas. */
    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    /** Pacientes registrados en un área clínica (odontología o pediatría). */
    public List<Paciente> listarPorArea(AreaClinica area) {
        if (area == null) {
            return pacienteRepository.findAll();
        }
        return pacienteRepository.findByArea(area);
    }

    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteRepository.findById(id);
    }

    public Optional<Paciente> buscarPorCurp(String curp, AreaClinica area) {
        return pacienteRepository.findByCurpAndArea(curp, area);
    }

    public List<Paciente> buscarPorNombreOApellido(String termino, AreaClinica area) {
        if (area == null) {
            return pacienteRepository.findAll().stream()
                    .filter(p -> contiene(p.getNombre(), termino) || contiene(p.getApellidos(), termino))
                    .toList();
        }
        return pacienteRepository
                .findByAreaAndNombreContainingIgnoreCaseOrAreaAndApellidosContainingIgnoreCase(
                        area, termino, area, termino);
    }

    private boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase().contains(termino.toLowerCase());
    }

    @Transactional
    public Paciente registrarOActualizarPaciente(Paciente paciente) {
        // Al editar, el paciente conserva el área en la que fue registrado
        if (paciente.getIdPaciente() != null) {
            pacienteRepository.findById(paciente.getIdPaciente())
                    .map(Paciente::getArea)
                    .ifPresent(paciente::setArea);
        }
        if (paciente.getArea() == null) {
            throw new IllegalArgumentException("No se pudo determinar el área clínica del paciente.");
        }

        // La CURP es única dentro de cada área: la misma persona puede estar en odontología y en pediatría
        if (paciente.getCurp() != null) {
            pacienteRepository.findByCurpAndArea(paciente.getCurp(), paciente.getArea()).ifPresent(existente -> {
                if (!existente.getIdPaciente().equals(paciente.getIdPaciente())) {
                    throw new IllegalArgumentException("Ya existe un paciente registrado con la CURP "
                            + paciente.getCurp() + " en el área de " + paciente.getArea().getEtiqueta() + ".");
                }
            });
        }

        // El tutor SIEMPRE se guarda primero: si ya tiene id, save() fusiona (merge)
        // los cambios editados desde el formulario. Corrige la persistencia al
        // editar datos del tutor desde el formulario del paciente.
        if (paciente.getTutor() != null) {
            // El tutor pertenece a la misma área que su paciente: nunca se comparte entre paneles
            paciente.getTutor().setArea(paciente.getArea());
            Tutor tutorGuardado = tutorRepository.save(paciente.getTutor());
            paciente.setTutor(tutorGuardado);
        }

        return pacienteRepository.save(paciente);
    }

    public List<Tutor> listarTutores() {
        return tutorRepository.findAll();
    }

    /** Tutores del área, para reutilizarlos sólo dentro del mismo panel. */
    public List<Tutor> listarTutoresPorArea(AreaClinica area) {
        if (area == null) {
            return tutorRepository.findAll();
        }
        return tutorRepository.findByArea(area);
    }

    @Transactional
    public Tutor guardarTutor(Tutor tutor) {
        return tutorRepository.save(tutor);
    }
}