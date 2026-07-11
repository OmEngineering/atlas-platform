package com.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.security.refresh-cookie")
public record RefreshCookieProperties(
        boolean secure,
        String sameSite,
        String path
) {
}
