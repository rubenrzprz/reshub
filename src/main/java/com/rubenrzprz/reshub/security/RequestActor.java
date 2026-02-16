package com.rubenrzprz.reshub.security;

import java.util.UUID;

public record RequestActor(
  UUID userId,
  Role role,
  UUID hotelId,
  UUID agencyId
) {
  public enum Role {
    MANAGER,
    RECEPTIONIST,
    AGENCY
  }
}
