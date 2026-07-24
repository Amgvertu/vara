package info.prorabka.varamy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaramyApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaramyApplication.class, args);
	}

}
