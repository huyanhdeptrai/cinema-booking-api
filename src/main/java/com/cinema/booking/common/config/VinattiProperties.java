package com.cinema.booking.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vinatti")
@Getter
@Setter
public class VinattiProperties {
    private String gatewayUrl;
    private String accessKey;
    private String secretKey;
    private Callback callback = new Callback();

    @Getter
    @Setter
    public static class Callback {
        private String keyId;
        private String secret;
    }
}
