package com.finere.scan_and_go_api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String privateKeyPath,
        String publicKeyPath,
        /** Raw PEM text, e.g. from a Koyeb/Heroku-style env var secret. Takes precedence over
         * the path-based properties above when set, since a cloud deployment has no gitignored
         * key file to point a path at. */
        String privateKeyPem,
        String publicKeyPem,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
