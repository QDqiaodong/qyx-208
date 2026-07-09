package com.example.geological;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GeologicalApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeologicalApplication.class, args);
    }
}