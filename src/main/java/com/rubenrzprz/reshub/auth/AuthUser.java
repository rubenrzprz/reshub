package com.rubenrzprz.reshub.auth;

import java.util.UUID;

public record AuthUser(
  UUID id,
  String email,
  String passwordHash,
  String role,
  UUID hotelId,
  UUID agencyId
) {
}
