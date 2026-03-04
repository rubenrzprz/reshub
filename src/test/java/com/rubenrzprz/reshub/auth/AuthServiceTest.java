package com.rubenrzprz.reshub.auth;

import com.rubenrzprz.reshub.api.ApiProblemException;
import com.rubenrzprz.reshub.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

  @Test
  void ambiguousCaseInsensitiveEmailMatchReturnsUnauthorized() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    JwtService jwtService = mock(JwtService.class);
    AuthService service = new AuthService(jdbc, jwtService);

    AuthUser first = new AuthUser(
      UUID.randomUUID(),
      "User@example.com",
      "$2a$10$4f2vK0OBj7N5kYQ9xJQfV.rkG2NqfA6Wq1f1YwM9EM5CYc2hiQ6bC",
      "MANAGER",
      UUID.randomUUID(),
      null
    );
    AuthUser second = new AuthUser(
      UUID.randomUUID(),
      "user@example.com",
      "$2a$10$4f2vK0OBj7N5kYQ9xJQfV.rkG2NqfA6Wq1f1YwM9EM5CYc2hiQ6bC",
      "MANAGER",
      UUID.randomUUID(),
      null
    );

    when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AuthUser>>any(), any()))
      .thenReturn(List.of(first, second));

    ApiProblemException ex = Assertions.assertThrows(
      ApiProblemException.class,
      () -> service.issueToken(new TokenRequest("user@example.com", "secret123"))
    );

    Assertions.assertEquals(401, ex.status().value());
    Assertions.assertEquals("invalid_credentials", ex.code());
  }
}
