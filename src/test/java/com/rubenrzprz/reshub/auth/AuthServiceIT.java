package com.rubenrzprz.reshub.auth;

import com.rubenrzprz.reshub.api.ApiProblemException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthServiceIT extends AuthApiIntegrationTestBase {

  @Autowired
  private AuthService authService;

  @Test
  void ambiguousCaseInsensitiveEmailMatchReturnsUnauthorized() {
    dataFactory.insertManagerUser(seed.hotelId(), "User@example.com", VALID_PASSWORD);
    dataFactory.insertManagerUser(seed.hotelId(), "user@example.com", VALID_PASSWORD);

    ApiProblemException ex = Assertions.assertThrows(
      ApiProblemException.class,
      () -> authService.issueToken(new TokenRequest("user@example.com", VALID_PASSWORD))
    );

    Assertions.assertEquals(401, ex.status().value());
    Assertions.assertEquals("invalid_credentials", ex.code());
  }
}
