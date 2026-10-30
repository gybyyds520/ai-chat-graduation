package com.example.aichat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.aichat.mapper")
public class AichatApplication {

	public static void main(String[] args) {
		SpringApplication.run(AichatApplication.class, args);
	}

}
