package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

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
        filterChain.doFilter(request, response);
        return;
      }

      Jws<Claims> parsed = jwtService.parseAndValidate(token);
      Claims claims = parsed.getPayload();
      RequestActor actor = toActor(claims);
      AuthenticatedActor principal = new AuthenticatedActor(actor);
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        principal,
        token,
        List.of(new SimpleGrantedAuthority("ROLE_" + actor.role().name()))
      );
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (ApiProblemException ex) {
      SecurityContextHolder.clearContext();
      resolver.resolveException(request, response, null, ex);
    } catch (JwtException | IllegalArgumentException ex) {
      SecurityContextHolder.clearContext();
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

  private RequestActor toActor(Claims claims) {
    UUID userId = parseUuid(claims.getSubject(), "subject");
    RequestActor.Role role = parseRole(claims.get("role", String.class));
    UUID hotelId = parseOptionalUuid(claims.get("hotelId", String.class), "hotelId");
    UUID agencyId = parseOptionalUuid(claims.get("agencyId", String.class), "agencyId");

    if (role == RequestActor.Role.AGENCY && agencyId == null) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "agency actor requires agencyId"
      );
    }

    if ((role == RequestActor.Role.MANAGER || role == RequestActor.Role.RECEPTIONIST) && hotelId == null) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "staff actor requires hotelId"
      );
    }

    return new RequestActor(userId, role, hotelId, agencyId);
  }

  private UUID parseOptionalUuid(String value, String claim) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseUuid(value, claim);
  }

  private UUID parseUuid(String value, String claim) {
    if (value == null || value.isBlank()) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "missing " + claim + " claim"
      );
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "invalid UUID in " + claim + " claim"
      );
    }
  }

  private RequestActor.Role parseRole(String value) {
    if (value == null || value.isBlank()) {
      throw new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "missing role claim");
    }
    try {
      return RequestActor.Role.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "invalid role claim");
    }
  }

}
