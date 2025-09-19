package com.app.shopin.config.cors;

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
                registry.addMapping("/api/**") // Aplica esta configuración a todas las rutas bajo /api
                        .allowedOrigins("http://localhost:3000") // Permite peticiones desde tu frontend en local
                        //.allowedOrigins("http://localhost:3000", "https://www.tuapp.com") // En producción, añadirías tu dominio real
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Métodos HTTP permitidos
                        .allowedHeaders("*") // Permite todas las cabeceras
                        .allowCredentials(true); // ¡CRUCIAL! Permite el envío de cookies (como JSESSIONID)
            }
        };
    }
}
