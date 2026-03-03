package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  public JwtAuthFilter(JwtService jwtService, JwtProperties jwtProperties) {
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Only reservations are protected for now
    return !path.startsWith("/reservations");
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    // Temporary bridge mode for existing tests/legacy clients
    if ((authHeader == null) || authHeader.isBlank() && jwtProperties.allowLegacyHeaders()) {
      String legacyUser = request.getHeader("X-User-Id");
      if (legacyUser != null && !legacyUser.isBlank()) {
        filterChain.doFilter(request, response);
        return;
      }
    }

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "missing bearer token"
      );
    }

    String token = authHeader.substring(BEARER_PREFIX.length()).trim();
    if (token.isBlank()) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "missing bearer token"
      );
    }

    try {
      Jws<Claims> parsed = jwtService.parseAndValidate(token);
      Claims claims = parsed.getPayload();

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);
      String hotelId = claims.get("hotelId", String.class);
      String agencyId = claims.get("agencyId", String.class);

      ActorHeaderRequestWrapper wrapped = new ActorHeaderRequestWrapper(request);
      wrapped.putHeader("X-User-Id", userId);
      wrapped.putHeader("X-Role", role);
      wrapped.putHeader("X-Hotel-Id", hotelId);
      wrapped.putHeader("X-Agency-Id", agencyId);

      filterChain.doFilter(wrapped, response);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "invalid bearer token"
      );
    }
  }

}
