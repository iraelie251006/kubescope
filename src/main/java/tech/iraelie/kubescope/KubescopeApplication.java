package tech.iraelie.kubescope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KubescopeApplication {

	public static void main(String[] args) {
		SpringApplication.run(KubescopeApplication.class, args);
	}

}
