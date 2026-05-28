package com.rubenrzprz.reshub.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreateRequest(
  @NotNull
  UUID hotelId,

  @NotNull
  UUID agencyId,

  UUID roomTypeId,

  @Size(max = 64)
  String externalRoomTypeCode,

  @Size(max = 160)
  String externalRoomTypeName,

  @NotBlank
  @Size(max = 64)
  String externalRef,

  @NotNull
  LocalDate arrivalDate,

  @NotNull
  LocalDate departureDate,

  @NotBlank
  @Size(max = 160)
  String guestName,

  @Min(1)
  Integer adults,

  @Min(0)
  Integer children,

  String notes
) {
}
