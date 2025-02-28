package com.group8.rbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RbsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RbsApplication.class, args);
	}

}
