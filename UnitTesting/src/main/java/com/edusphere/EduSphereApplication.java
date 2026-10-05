package com.edusphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * EduSphere — Spring Boot Application Entry Point
 *
 * Extends SpringBootServletInitializer để hỗ trợ deploy dưới dạng WAR
 * lên external Tomcat (ngoài embedded Tomcat).
 */
@SpringBootApplication
public class EduSphereApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EduSphereApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(EduSphereApplication.class, args);
    }
}
