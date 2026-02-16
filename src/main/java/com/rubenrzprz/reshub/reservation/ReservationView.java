package com.rubenrzprz.reshub.reservation;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationView(
  UUID id,
  UUID hotelId,
  UUID agencyId,
  UUID createdByUserId,
  String status,
  LocalDate arrivalDate,
  LocalDate departureDate,
  String guestName,
  String notes
) {}
