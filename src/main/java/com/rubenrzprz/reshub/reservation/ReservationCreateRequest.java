package com.rubenrzprz.reshub.reservation;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreateRequest(
  UUID hotelId,
  UUID agencyId,
  UUID roomTypeId,
  String externalRoomTypeCode,
  String externalRoomTypeName,
  String externalRef,
  LocalDate arrivalDate,
  LocalDate departureDate,
  String guestName,
  Integer adults,
  Integer children,
  String notes
) {
}
