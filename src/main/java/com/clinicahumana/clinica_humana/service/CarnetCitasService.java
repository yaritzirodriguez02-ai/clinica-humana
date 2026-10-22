package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.CarnetCitas;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.repository.CarnetCitasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio CRUD para el carnet de citas / carnet de atención (CISO).
 */
@Service
public class CarnetCitasService {

    @Autowired
    private CarnetCitasRepository carnetRepo;

    public List<CarnetCitas> listarTodos() {
        return carnetRepo.findAll();
    }

    public Optional<CarnetCitas> buscarPorId(Long id) {
        return carnetRepo.findById(id);
    }

    public Optional<CarnetCitas> buscarPorIdCarnet(Long idCarnet) {
        return carnetRepo.findByIdCarnet(idCarnet);
    }

    public List<CarnetCitas> listarPorPaciente(Long idPaciente) {
        return carnetRepo.findByPacienteIdPaciente(idPaciente);
    }

    public List<CarnetCitas> listarPorPacienteEntity(Paciente paciente) {
        return carnetRepo.findByPaciente(paciente);
    }

    public long contarPorPaciente(Long idPaciente) {
        return carnetRepo.countByPacienteIdPaciente(idPaciente);
    }

    @Transactional
    public CarnetCitas guardarCarnet(CarnetCitas carnet) {
        return carnetRepo.save(carnet);
    }

    @Transactional
    public CarnetCitas crearCarnet(Long idPaciente, LocalDate fecha, LocalTime hora,
                                   String tratamientoRealizar, BigDecimal totalPago) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(idPaciente);

        CarnetCitas carnet = new CarnetCitas();
        carnet.setPaciente(paciente);
        carnet.setFecha(fecha);
        carnet.setHora(hora);
        carnet.setTratamientoRealizar(tratamientoRealizar);
        carnet.setTotalPago(totalPago);
        return carnetRepo.save(carnet);
    }

    @Transactional
    public void eliminarCarnet(Long id) {
        carnetRepo.deleteById(id);
    }

    @Transactional
    public boolean existePorIdCarnet(Long idCarnet) {
        return carnetRepo.findByIdCarnet(idCarnet).isPresent();
    }
}
