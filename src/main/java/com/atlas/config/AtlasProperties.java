package com.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.api")
public record AtlasProperties(String basePath) {
}
