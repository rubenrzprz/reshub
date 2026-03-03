package com.rubenrzprz.reshub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
  String secret,
  String issuer,
  long expirationMinutes,
  boolean allowLegacyHeaders
) {
  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("security.jwt.secret must be configured via JWT_SECRET and cannot be blank");
    }
    if (secret.length() < 32) {
      throw new IllegalStateException("security.jwt.secret must be at least 32 characters long");
    }
  }
}
