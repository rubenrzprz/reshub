package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final int BEARER_PREFIX_LENGTH = 7; // "Bearer "

  private final JwtService jwtService;
  private final HandlerExceptionResolver resolver;

  public JwtAuthFilter(
    JwtService jwtService,
    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
  ) {
    this.jwtService = jwtService;
    this.resolver = resolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    // Only reservations are protected for now
    return !path.startsWith("/reservations");
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String token = extractBearerToken(request.getHeader("Authorization"));
      if (token == null) {
        throw new ApiProblemException(
          HttpStatus.UNAUTHORIZED,
          "unauthorized_actor_context",
          "missing bearer token"
        );
      }

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
    } catch (ApiProblemException ex) {
      resolver.resolveException(request, response, null, ex);
    } catch (JwtException | IllegalArgumentException ex) {
      resolver.resolveException(
        request,
        response,
        null,
        new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "invalid bearer token")
      );
    }
  }

  private String extractBearerToken(String authHeader) {
    if (authHeader == null || authHeader.length() <= BEARER_PREFIX_LENGTH) {
      return null;
    }
    if (!authHeader.regionMatches(true, 0, "Bearer ", 0, BEARER_PREFIX_LENGTH)) {
      return null;
    }
    String token = authHeader.substring(BEARER_PREFIX_LENGTH).trim();
    return token.isBlank() ? null : token;
  }

}
