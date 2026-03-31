package com._team._team;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "com._team._team")
@EnableJpaAuditing
<<<<<<< HEAD:member-service/src/main/java/com/_team/_team/Application.java
@EnableAspectJAutoProxy
public class Application {
=======
public class MemberApplication {
>>>>>>> 090ea24 (first):member-service/src/main/java/com/_team/_team/MemberApplication.java

	public static void main(String[] args) {
		SpringApplication.run(MemberApplication.class, args);
	}

}
