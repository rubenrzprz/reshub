package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestActorResolver {

  public RequestActor resolve() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null ||
        !authentication.isAuthenticated() ||
        !(authentication.getPrincipal() instanceof AuthenticatedActor principal)
    ) {
      throw new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "missing authenticated actor");
    }

    return principal.actor();
  }
}
