package net.behavr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "net.behavr")
public class ApiApplication {

	public static void main(String[] args) {
		DotenvBootstrap.apply();
		SpringApplication.run(ApiApplication.class, args);
	}

}
