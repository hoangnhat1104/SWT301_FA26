package com.edusphere.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring MVC.
 * Map URL /uploads/avatars/** -> thư mục upload thực tế trên disk.
 * Cho phép browser load ảnh avatar đã upload.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static assets từ webapp/assets — map đúng đường dẫn thực tế
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/assets/");

        // Avatar uploads từ thư mục trên disk
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
