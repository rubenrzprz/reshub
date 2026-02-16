package com.rubenrzprz.reshub.reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationCommentView(
  UUID id,
  UUID reservationId,
  UUID authorUserId,
  String body,
  OffsetDateTime createdAt
) {
}
