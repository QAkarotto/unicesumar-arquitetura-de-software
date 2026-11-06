package br.edu.foodnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FoodNowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodNowApplication.class, args);
    }
}
