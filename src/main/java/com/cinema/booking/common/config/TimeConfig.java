package com.cinema.booking.common.config;

import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Bean
    public ValidationConfigurationCustomizer validationClockCustomizer(
            Clock clock
    ) {
        return configuration ->
                configuration.clockProvider(() -> clock);
    }
}