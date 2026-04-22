package com.project.sls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlsApplication.class, args);
	}

}
