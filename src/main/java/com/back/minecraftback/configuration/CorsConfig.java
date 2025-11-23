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
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")    // вместо allowedOrigins("*")
                        .allowedMethods("*")           // любые методы
                        .allowedHeaders("*")           // любые заголовки
                        .allowCredentials(true);       // разрешить куки
            }
        };
    }
}

