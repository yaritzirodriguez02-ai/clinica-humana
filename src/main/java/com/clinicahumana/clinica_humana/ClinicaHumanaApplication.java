package com.clinicahumana.clinica_humana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClinicaHumanaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClinicaHumanaApplication.class, args);
	}

}
