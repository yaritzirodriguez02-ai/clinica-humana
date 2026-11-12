package com.clinicahumana.clinica_humana.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Procesa los formularios multipart (subida de fotos) ANTES de la seguridad,
     * para que el token CSRF viaje dentro del formulario sin ser rechazado.
     */
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilter() {
        FilterRegistrationBean<MultipartFilter> registro = new FilterRegistrationBean<>(new MultipartFilter());
        // -101: justo antes de la cadena de Spring Security (orden -100)
        registro.setOrder(-101);
        return registro;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos públicos
                .requestMatchers("/css/**", "/js/**", "/images/**", "/portal-citas/**").permitAll()

                // Portal Público de Citas para Tutores (agendamiento en línea SIN sesión):
                // incluye /public/agendar-cita y sus endpoints JSON de médicos y horarios ocupados
                .requestMatchers("/public/**").permitAll()
                
                // Panel de administración y gestión de personal: EXCLUSIVO de ADMIN
                .requestMatchers("/admin/**", "/empleados/**").hasRole("ADMIN")

                // Módulo CU01 (Pacientes y Tutores): Acceso para todos los roles autorizados
                .requestMatchers("/pacientes/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "PEDIATRA", "ODONTOPEDIATRA")
                
                // Módulo CU02 (Citas / Agenda): Acceso por rol, sin cruce entre paneles clínicos
                .requestMatchers("/citas/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "RECEPCIONISTA", "ADMIN")
                
                // Módulo CU03 (Consulta Pediátrica): Pediatra y Admin
                .requestMatchers("/consultas/pediatria/**").hasAnyRole("PEDIATRA", "ADMIN")
                
                // Módulo CU04 (Consulta Odontopediátrica): Odontopediatra y Admin
                .requestMatchers("/consultas/odontopediatria/**").hasAnyRole("ODONTOPEDIATRA", "ADMIN")
                
                // Apertura de expediente desde la agenda (/consultas/atender/{id}): solo personal médico
                .requestMatchers("/consultas/atender/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "ADMIN")

                // Recetas médicas: solo personal médico (Recepción no emite recetas)
                .requestMatchers("/recetas/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "ADMIN")

                // Informes en hoja membretada de CISO: la odontóloga los redacta,
                // recepción los consulta/imprime y el administrador tiene acceso total
                .requestMatchers("/informes/**").hasAnyRole("ODONTOLOGIA", "ODONTOPEDIATRA", "RECEPCIONISTA", "ADMIN")

                // Carnet de citas del consultorio (CISO): lo imprime la odontóloga,
                // recepción y el administrador también pueden reimprimirlo
                .requestMatchers("/carnets/**").hasAnyRole("ODONTOLOGIA", "ODONTOPEDIATRA", "RECEPCIONISTA", "ADMIN")

                // Limpieza de registros por área: odontología y recepción comparten el
                // área dental, pediatría administra la suya y el admin controla ambas
                .requestMatchers("/registros/**").hasAnyRole("ODONTOLOGIA", "ODONTOPEDIATRA", "RECEPCIONISTA", "PEDIATRA", "ADMIN")

                // Catálogo de medicamentos (Vademécum interno): personal médico y admin
                .requestMatchers("/medicamentos/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "ADMIN")

                // Módulo CU06 (Cobros, Caja y Corte): RECEPCIÓN, ADMIN y MÉDICOS
                .requestMatchers("/pagos/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "PEDIATRA", "ODONTOPEDIATRA")
                
                // El resto de rutas requiere sesión iniciada
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().replace("ROLE_", "").equalsIgnoreCase("ADMIN"));
                    
                    if (isAdmin) {
                        response.sendRedirect("/admin/dashboard");
                    } else {
                        response.sendRedirect("/citas");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // CSRF queda ACTIVO (protección por defecto de Spring Security); los formularios
            // Thymeleaf con th:action envían el token _csrf automáticamente
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}