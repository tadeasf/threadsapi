package com.tadeasfort.threadsapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

        @Value("${app.url}")
        private String appUrl;

        @Bean
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                // CORS for API endpoints
                registry.addMapping("/api/**")
                                .allowedOrigins(
                                                appUrl, // Production URL
                                                "http://localhost:3001", // Frontend dev server (corrected port)
                                                "http://localhost:10081" // Backend dev server
                                )
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true);

                // CORS for webhook endpoints (allow Meta's servers)
                registry.addMapping("/api/auth/uninstall")
                                .allowedOrigins("*") // Meta can call from various IPs
                                .allowedMethods("GET", "POST", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(false);

                registry.addMapping("/api/auth/delete")
                                .allowedOrigins("*") // Meta can call from various IPs
                                .allowedMethods("GET", "POST", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(false);
        }
}