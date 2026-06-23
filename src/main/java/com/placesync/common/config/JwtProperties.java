package com.placesync.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret = "dev-secret-key-change-in-production-32ch";
    private long accessTokenExpiryMs = 900_000L;
    private int refreshTokenExpiryDays = 7;
}
