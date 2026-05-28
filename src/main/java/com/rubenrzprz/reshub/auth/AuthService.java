package com.rubenrzprz.reshub.auth;

import com.rubenrzprz.reshub.api.ApiProblemException;
import com.rubenrzprz.reshub.persistence.AppUserEntity;
import com.rubenrzprz.reshub.persistence.AppUserRepository;
import com.rubenrzprz.reshub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

  private final AppUserRepository appUserRepository;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(AppUserRepository appUserRepository, JwtService jwtService) {
    this.appUserRepository = appUserRepository;
    this.jwtService = jwtService;
  }

  public TokenResponse issueToken(TokenRequest request) {
    validateRequest(request);

    AuthUser user = findByEmail(request.email());
    if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
      throw invalidCredentials();
    }

    String token = jwtService.generateToken(user);
    return new TokenResponse(token, "Bearer", jwtService.expiresInSeconds());
  }

  private void validateRequest(TokenRequest request) {
    if (request == null ||
      request.email() == null || request.email().isBlank() ||
      request.password() == null || request.password().isBlank()) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_auth_payload", "email and password are required");
    }
  }

  private AuthUser findByEmail(String email) {
    String normalized = email.toLowerCase(Locale.ROOT).trim();

    List<AppUserEntity> rows = appUserRepository.findByEmailIgnoreCase(normalized);

    if (rows.isEmpty()) {
      throw invalidCredentials();
    }
    if (rows.size() > 1) {
      throw invalidCredentials();
    }

    AppUserEntity user = rows.getFirst();
    return new AuthUser(
      user.getId(),
      user.getEmail(),
      user.getPasswordHash(),
      user.getRole(),
      user.getHotelId(),
      user.getAgencyId()
    );
  }

  private ApiProblemException invalidCredentials() {
    return new ApiProblemException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "invalid credentials");
  }

}
