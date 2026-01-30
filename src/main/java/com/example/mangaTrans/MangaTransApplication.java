package com.example.mangaTrans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MangaTransApplication {

	public static void main(String[] args) {
		SpringApplication.run(MangaTransApplication.class, args);
	}

}
