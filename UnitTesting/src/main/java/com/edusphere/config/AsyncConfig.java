package com.edusphere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật @Async để EmailService gửi email không block luồng chính.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
