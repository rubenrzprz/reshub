package com.rubenrzprz.reshub.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

  private final JwtAuthFilter filter = new JwtAuthFilter(
    mock(JwtService.class),
    mock(HandlerExceptionResolver.class)
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
