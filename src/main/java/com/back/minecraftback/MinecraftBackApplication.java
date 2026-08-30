package com.back.minecraftback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinecraftBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinecraftBackApplication.class, args);
    }

}
