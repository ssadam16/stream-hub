package com.technokratos.codeexecutorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.technokratos.common",
        "com.technokratos.codeexecutorservice"
})
public class CodeExecutorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeExecutorServiceApplication.class, args);
    }
}
