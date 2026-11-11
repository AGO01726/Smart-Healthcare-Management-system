package com.smarthealth.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injected wherever "now" is needed so the time source is explicit. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
