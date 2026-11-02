package gr.fotistsou.koinoxrista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KoinoxristaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoinoxristaApplication.class, args);
    }

}
