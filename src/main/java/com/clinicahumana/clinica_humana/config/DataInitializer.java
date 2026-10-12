package com.clinicahumana.clinica_humana.config;

import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra las cuentas iniciales del sistema en una base de datos limpia de MySQL.
 * Se ejecuta al arrancar la aplicación y solo crea usuarios que todavía no existen
 * (idempotente: no duplica ni sobreescribe cuentas existentes).
 *
 * Las contraseñas se guardan codificadas con el PasswordEncoder de la aplicación
 * (DelegatingPasswordEncoder, que usa BCrypt internamente y produce hashes {bcrypt}...).
 *
 * IMPORTANTE: en producción debe cambiarse la contraseña tras el primer acceso.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Clave de las cuentas de demostración (coincide con la ayuda mostrada en login.html). */
    private static final String CLAVE_DEMO = "123456";

    /** Clave del administrador: más robusta por ser el rol con acceso total. */
    private static final String CLAVE_ADMIN = "Admin@2026";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("=== Clínica Humana: verificando usuarios iniciales ===");

        // 1. Administrador general (panel, personal, reportes)
        crearUsuarioSiNoExiste("admin", "Administrador General", "ADMIN", CLAVE_ADMIN);

        // 2. Recepción y caja (cobros y agenda de odontopediatría)
        crearUsuarioSiNoExiste("recepcion", "Recepción y Caja", "RECEPCIONISTA", CLAVE_DEMO);

        // 3. Médicas pediatras
        crearUsuarioSiNoExiste("pilar.rios", "Dra. Pilar Ríos", "PEDIATRA", CLAVE_DEMO);
        crearUsuarioSiNoExiste("laura.mendez", "Dra. Laura Méndez", "PEDIATRA", CLAVE_DEMO);

        // 4. Médicas odontopediatras
        crearUsuarioSiNoExiste("gloria.gonzalez", "Dra. Gloria González", "ODONTOPEDIATRA", CLAVE_DEMO);
        crearUsuarioSiNoExiste("sofia.castro", "Dra. Sofía Castro", "ODONTOPEDIATRA", CLAVE_DEMO);

        log.info("=== Usuarios iniciales listos | admin: '{}' | cuentas demo: '{}' ===",
                CLAVE_ADMIN, CLAVE_DEMO);
    }

    private void crearUsuarioSiNoExiste(String username, String nombreCompleto, String rol, String passwordPlano) {
        if (usuarioRepository.existsByUsername(username)) {
            log.info("El usuario '{}' ya existe en la BD; se omite.", username);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(username.replace(".", "") + "@clinicahumana.mx");
        usuario.setRol(rol);
        usuario.setPassword(passwordEncoder.encode(passwordPlano)); // hash {bcrypt}...
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        log.info("Usuario creado -> username: '{}', rol: '{}'", username, rol);
    }
}
