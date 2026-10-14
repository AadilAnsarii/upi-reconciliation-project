package com.jpb.reconciliation.reconciliation;

import org.springframework.boot.SpringApplication; 
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;


@SpringBootApplication()
@EnableScheduling
@EnableRetry
@EnableEncryptableProperties
public class ReconciliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconciliationApplication.class, args);
		System.out.println("Recon Running!");
	}
}
