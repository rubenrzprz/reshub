package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.auth.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

  private final JwtProperties props;
  private final Key signingKey;

  public JwtService(JwtProperties props) {
    this.props = props;
    this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(AuthUser user) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.expirationMinutes(), ChronoUnit.MINUTES);

    return Jwts.builder()
      .subject(user.id().toString())
      .issuer(props.issuer())
      .issuedAt(Date.from(now))
      .expiration(Date.from(exp))
      .claim("role", user.role())
      .claim("hotelId", user.hotelId() == null ? null : user.hotelId().toString())
      .claim("agencyId", user.agencyId() == null ? null : user.agencyId().toString())
      .signWith(signingKey)
      .compact();
  }

  public Jws<Claims> parseAndValidate(String token) {
    return Jwts.parser()
      .verifyWith((javax.crypto.SecretKey) signingKey)
      .requireIssuer(props.issuer())
      .build()
      .parseSignedClaims(token);
  }

  public long expiresInSeconds() {
    return props.expirationMinutes() * 60;
  }
}
