package com._team._team;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "com._team._team")
@EnableJpaAuditing
public class GoalApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoalApplication.class, args);
	}

}
