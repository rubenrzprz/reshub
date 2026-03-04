package com.rubenrzprz.reshub.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

class JwtAuthFilterTest {

  private static final String TEST_JWT_SECRET = "test-secret-key-with-at-least-32-characters";
  private final JwtAuthFilter filter = new JwtAuthFilter(
    new JwtService(new JwtProperties(TEST_JWT_SECRET, "reshub-test", 60)),
    (request, response, handler, ex) -> null
  );

  @Test
  void reservationsPathWithContextPathIsStillProtected() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/api");
    request.setRequestURI("/api/reservations");

    Assertions.assertFalse(filter.shouldNotFilter(request));
  }

  @Test
  void nonReservationPathIsNotProtected() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/api");
    request.setRequestURI("/api/health");

    Assertions.assertTrue(filter.shouldNotFilter(request));
  }
}
