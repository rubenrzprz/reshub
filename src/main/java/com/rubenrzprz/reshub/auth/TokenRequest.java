package com.rubenrzprz.reshub.auth;

public record TokenRequest(
  String email,
  String password
) {
}
