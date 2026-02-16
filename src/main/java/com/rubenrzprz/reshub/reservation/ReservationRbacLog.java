package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.util.UUID;

final class ReservationRbacLog {

  static final String EVENT_READ_AUTHORIZED = "reservation.read.authorized";
  static final String EVENT_READ_FORBIDDEN = "reservation.read.forbidden";
  static final String EVENT_READ_NOT_FOUND = "reservation.read.not_found";

  static final String REASON_HOTEL_SCOPE_MISMATCH = "hotel_scope_mismatch";
  static final String REASON_AGENCY_SCOPE_MISMATCH = "agency_scope_mismatch";
  static final String REASON_UNSUPPORTED_ROLE = "unsupported_role";

  private ReservationRbacLog() {
  }

  static String fields(RequestActor actor, UUID reservationId, String decision, String reason) {
    return "event=" + ("ALLOW".equals(decision) ? EVENT_READ_AUTHORIZED : EVENT_READ_FORBIDDEN) +
      " actorUserId=" + actor.userId() +
      " actorRole=" + actor.role() +
      " actorHotelId=" + actor.hotelId() +
      " actorAgencyId=" + actor.agencyId() +
      " reservationId=" + reservationId +
      " decision=" + decision +
      " reason=" + reason;
  }
}
