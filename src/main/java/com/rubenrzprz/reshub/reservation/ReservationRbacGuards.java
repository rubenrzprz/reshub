package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.util.UUID;
import java.util.function.Consumer;

final class ReservationRbacGuards {

  private ReservationRbacGuards() {
  }

  static void requireHotelScope(RequestActor actor, UUID hotelId, Consumer<String> deny) {
    if (!hotelId.equals(actor.hotelId())) {
      deny.accept(ReservationRbacLog.REASON_HOTEL_SCOPE_MISMATCH);
    }
  }

  static void requireAgencyScope(RequestActor actor, UUID agencyId, Consumer<String> deny) {
    if (!agencyId.equals(actor.agencyId())) {
      deny.accept(ReservationRbacLog.REASON_AGENCY_SCOPE_MISMATCH);
    }
  }

  static void requireOwner(RequestActor actor, UUID ownerUserId, Consumer<String> deny) {
    if (!ownerUserId.equals(actor.userId())) {
      deny.accept(ReservationRbacLog.REASON_OWNER_MISMATCH);
    }
  }
}
