package upao.edu.pe.TurismoDiasAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TurismoDiasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TurismoDiasApiApplication.class, args);
	}

}
