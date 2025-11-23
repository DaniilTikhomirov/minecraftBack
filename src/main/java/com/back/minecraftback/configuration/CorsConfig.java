package com.back.minecraftback.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")            // все эндпоинты
                        .allowedOriginPatterns("*")    // любой фронт
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // разрешаем OPTIONS
                        .allowedHeaders("*")
                        .allowCredentials(true);       // для cookies
            }
        };
    }
}


