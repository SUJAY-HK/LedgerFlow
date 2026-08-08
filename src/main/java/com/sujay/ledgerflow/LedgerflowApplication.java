package com.sujay.ledgerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LedgerflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerflowApplication.class, args);
	}

}
