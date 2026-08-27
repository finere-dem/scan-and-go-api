package com.finere.scan_and_go_api.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qr")
public record QrProperties(String hmacSecret, String baseUri) {
}
