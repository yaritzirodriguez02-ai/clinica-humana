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
                
                // Módulo CU01 (Pacientes): Recepción, Admin y Especialistas
                .requestMatchers("/pacientes/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "PEDIATRA", "ODONTOPEDIATRA")
                
                // Módulo CU02 (Citas / Agenda): Todos los roles autenticados
                .requestMatchers("/citas/**").authenticated()
                
                // Módulo CU03 (Consulta Pediátrica): Exclusivo Pediatra y Admin
                .requestMatchers("/consultas/pediatria/**").hasAnyRole("PEDIATRA", "ADMIN")
                
                // Módulo CU04 (Consulta Odontopediátrica): Exclusivo Odontopediatra y Admin
                .requestMatchers("/consultas/odontopediatria/**").hasAnyRole("ODONTOPEDIATRA", "ADMIN")
                
                // Cobros / Caja (CU06): Recepción y Admin
                .requestMatchers("/pagos/**").hasAnyRole("RECEPCIONISTA", "ADMIN")
                
                // El resto requiere estar autenticado
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
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}