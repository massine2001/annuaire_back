package org.massine.annuaire_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication
public class AnnuaireBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnnuaireBackApplication.class, args);
	}

}
