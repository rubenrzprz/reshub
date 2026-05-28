package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class SecurityProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final HandlerExceptionResolver resolver;

  public SecurityProblemAuthenticationEntryPoint(
    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
  ) {
    this.resolver = resolver;
  }

  @Override
  public void commence(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException authException
  ) throws IOException {
    resolver.resolveException(
      request,
      response,
      null,
      new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "missing bearer token")
    );
  }
}
