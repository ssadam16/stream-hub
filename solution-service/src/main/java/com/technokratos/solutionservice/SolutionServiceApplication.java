package com.technokratos.solutionservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
        servers = {@Server(url = "http://localhost:8080", description = "Gateway")}
)
@SpringBootApplication(scanBasePackages = {
        "com.technokratos.common",
        "com.technokratos.solutionservice"
})
public class SolutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolutionServiceApplication.class, args);
    }

}
