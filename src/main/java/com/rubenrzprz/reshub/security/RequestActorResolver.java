package com.rubenrzprz.reshub.security;

import com.rubenrzprz.reshub.api.ApiProblemException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RequestActorResolver {

  public RequestActor resolve(
    String userIdHeader,
    String roleHeader,
    String hotelIdHeader,
    String agencyIdHeader
  ) {
    if (userIdHeader == null || userIdHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) {
      throw new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "missing actor headers");
    }

    UUID userId = parseUuid(userIdHeader, "X-User-Id");
    RequestActor.Role role = parseRole(roleHeader);
    UUID hotelId = parseOptionalUuid(hotelIdHeader, "X-Hotel-Id");
    UUID agencyId = parseOptionalUuid(agencyIdHeader, "X-Agency-Id");

    if (role == RequestActor.Role.AGENCY && agencyId == null) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "agency actor requires X-Agency-Id"
      );
    }

    if ((role == RequestActor.Role.MANAGER || role == RequestActor.Role.RECEPTIONIST) && hotelId == null) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "staff actor requires X-Hotel-Id"
      );
    }

    return new RequestActor(userId, role, hotelId, agencyId);
  }

  private UUID parseOptionalUuid(String value, String header) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseUuid(value, header);
  }

  private UUID parseUuid(String value, String header) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ApiProblemException(
        HttpStatus.UNAUTHORIZED,
        "unauthorized_actor_context",
        "invalid UUID in " + header
      );
    }
  }

  private RequestActor.Role parseRole(String value) {
    try {
      return RequestActor.Role.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ApiProblemException(HttpStatus.UNAUTHORIZED, "unauthorized_actor_context", "invalid X-Role");
    }
  }
}
