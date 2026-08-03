package com.sujay.ledgerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication
public class LedgerflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerflowApplication.class, args);
//		System.out.println("LedgerflowApplication started successfully.");
	}

}

