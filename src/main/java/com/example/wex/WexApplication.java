package com.example.wex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WexApplication {
    public static void main(String[] args) {
        SpringApplication.run(WexApplication.class, args);
    }
}
