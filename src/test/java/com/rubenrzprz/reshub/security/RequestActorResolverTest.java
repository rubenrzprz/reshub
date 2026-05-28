package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestActorResolverTest {

  private final RequestActorResolver resolver = new RequestActorResolver();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void resolvesActorFromSecurityContext() {
    RequestActor actor = new RequestActor(
      UUID.randomUUID(),
      RequestActor.Role.MANAGER,
      UUID.randomUUID(),
      null
    );
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(
        new AuthenticatedActor(actor),
        "token",
        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
      )
    );

    Assertions.assertEquals(actor, resolver.resolve());
  }

  @Test
  void missingAuthenticationIsUnauthorized() {
    ApiProblemException exception = Assertions.assertThrows(ApiProblemException.class, resolver::resolve);

    Assertions.assertEquals("unauthorized_actor_context", exception.code());
  }
}
