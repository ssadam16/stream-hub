package com.technokratos.solutionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.technokratos.common",
        "com.technokratos.solutionservice"
})
public class SolutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolutionServiceApplication.class, args);
    }

}
