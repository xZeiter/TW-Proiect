package com.smartgrade.smartgrade_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        registry.addResourceHandler("/crops/**")
                .addResourceLocations("file:///C:/Proiect6/Proiect/scanner/storage/crops/");
    }
}