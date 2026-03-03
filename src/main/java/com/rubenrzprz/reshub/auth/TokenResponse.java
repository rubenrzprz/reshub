package com.rubenrzprz.reshub.auth;

public record TokenResponse(
  String accessToken,
  String tokenType,
  long expiresInSeconds
) {
}
