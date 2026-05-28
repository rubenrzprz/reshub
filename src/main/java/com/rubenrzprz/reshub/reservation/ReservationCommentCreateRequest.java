package com.rubenrzprz.reshub.reservation;

import jakarta.validation.constraints.NotBlank;

public record ReservationCommentCreateRequest(
  @NotBlank
  String body
) {
}
