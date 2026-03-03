package com.rubenrzprz.reshub.auth;

import com.rubenrzprz.reshub.api.ApiProblemException;
import com.rubenrzprz.reshub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

  private final JdbcTemplate jdbc;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(JdbcTemplate jdbc, JwtService jwtService) {
    this.jdbc = jdbc;
    this.jwtService = jwtService;
  }

  public TokenResponse issueToken(TokenRequest request) {
    validateRequest(request);

    AuthUser user = findByEmail(request.email());
    if(!passwordEncoder.matches(request.password(), user.passwordHash())) {
      throw invalidCredentials();
    }

    String token = jwtService.generateToken(user);
    return new TokenResponse(token, "Bearer", jwtService.expiresInSeconds());
  }

  private void validateRequest(TokenRequest request) {
    if(request == null ||
    request.email() == null || request.email().isBlank() ||
    request.password() == null || request.password().isBlank()) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_auth_paylooad", "email and password are required");
    }
  }

  private AuthUser findByEmail(String email) {
    String normalized = email.toLowerCase(Locale.ROOT).trim();

    List<AuthUser> rows = jdbc.query(
      "select id, email, password_hash, role, hotel_id, agency_id from app_user where lower(email) = ?",
      (rs, rowNum) -> new AuthUser(
        UUID.fromString(rs.getString("id")),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("role"),
        rs.getString("hotel_id") == null ? null : UUID.fromString(rs.getString("hotel_id")),
        rs.getString("agency_id") == null ? null : UUID.fromString(rs.getString("agency_id"))
      ),
      normalized
    );

    if(rows.isEmpty()) {
      throw invalidCredentials();
    }

    return rows.getFirst();
  }

  private ApiProblemException invalidCredentials() {
    return new ApiProblemException(HttpStatus.UNAUTHORIZED, "invalid_credentails", "invalid credentials");
  }

}
