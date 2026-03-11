package com.backend.aurum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AurumApplication {
	public static void main(String[] args) {
		SpringApplication.run(AurumApplication.class, args);
	}
}
