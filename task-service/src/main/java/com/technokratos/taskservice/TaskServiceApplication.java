package com.technokratos.taskservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@OpenAPIDefinition(
        servers = {@Server(url = "http://localhost:8080", description = "Gateway")}
)
@SpringBootApplication
@ComponentScan(basePackages = {
		"com.technokratos.taskservice",
		"com.technokratos.common"
})
public class TaskServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskServiceApplication.class, args);
	}

}
