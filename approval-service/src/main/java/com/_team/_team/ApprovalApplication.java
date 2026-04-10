package com._team._team;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ApprovalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApprovalApplication.class, args);
	}

}
