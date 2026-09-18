package com.example.demo;

import com.example.demo.config.OpenRouterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
// Senza questa riga OpenRouterProperties resterebbe una classe qualunque:
// e' cio' che dice a Spring di popolarla con le properties "openrouter.*"
// e di registrarla come bean iniettabile.
@EnableConfigurationProperties(OpenRouterProperties.class)
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
