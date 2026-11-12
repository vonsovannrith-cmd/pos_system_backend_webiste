package com.example.pos_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PosSystemApplication {

	public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Phnom_Penh"));
        SpringApplication.run(PosSystemApplication.class, args);
	}

}
