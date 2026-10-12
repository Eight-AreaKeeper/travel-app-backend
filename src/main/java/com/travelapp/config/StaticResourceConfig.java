package com.travelapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exposes the local disk directory specified by app.upload.dir as a static resource via
 * /uploads/**, working with FileStorageService to implement the image upload/display feature
 * in 2.6. Once production switches to object storage, uploaded files will have direct CDN URLs
 * and this mapping will no longer be needed.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadDir.replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
