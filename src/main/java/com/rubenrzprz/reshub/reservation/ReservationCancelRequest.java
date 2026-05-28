package com.rubenrzprz.reshub.reservation;

import jakarta.validation.constraints.Size;

public record ReservationCancelRequest(
  @Size(max = 160)
  String reason
) {
}
