package com.example.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * Application configuration beans.
 *
 * RestClient.Builder is a Spring-managed prototype bean — each HttpInventoryClient
 * gets its own builder instance so it can set its own baseUrl without affecting others.
 *
 * @EnableScheduling activates @Scheduled method scanning.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
