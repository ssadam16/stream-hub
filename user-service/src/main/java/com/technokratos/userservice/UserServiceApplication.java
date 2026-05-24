package com.technokratos.userservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;


@OpenAPIDefinition(
        servers = {@Server(url = "http://localhost:8080", description = "Gateway")}
)
@SpringBootApplication(scanBasePackages = {
        "com.technokratos.common",
        "com.technokratos.userservice"
})
@EnableKafka
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
