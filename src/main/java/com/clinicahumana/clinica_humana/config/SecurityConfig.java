package com.clinicahumana.clinica_humana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos públicos
                .requestMatchers("/css/**", "/js/**", "/images/**", "/portal-citas/**").permitAll()
                
                // Panel de administración y gestión de personal: EXCLUSIVO de ADMIN
                .requestMatchers("/admin/**", "/empleados/**").hasRole("ADMIN")

                // Módulo CU01 (Pacientes y Tutores): Acceso para todos los roles autorizados
                .requestMatchers("/pacientes/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "PEDIATRA", "ODONTOPEDIATRA")
                
                // Módulo CU02 (Citas / Agenda): Acceso para todos los roles autenticados
                .requestMatchers("/citas/**").authenticated()
                
                // Módulo CU03 (Consulta Pediátrica): Pediatra y Admin
                .requestMatchers("/consultas/pediatria/**").hasAnyRole("PEDIATRA", "ADMIN")
                
                // Módulo CU04 (Consulta Odontopediátrica): Odontopediatra y Admin
                .requestMatchers("/consultas/odontopediatria/**").hasAnyRole("ODONTOPEDIATRA", "ADMIN")
                
                // Apertura de expediente desde la agenda (/consultas/atender/{id}): solo personal médico
                .requestMatchers("/consultas/atender/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "ADMIN")

                // Recetas médicas: solo personal médico (Recepción no emite recetas)
                .requestMatchers("/recetas/**").hasAnyRole("PEDIATRA", "ODONTOPEDIATRA", "ADMIN")

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