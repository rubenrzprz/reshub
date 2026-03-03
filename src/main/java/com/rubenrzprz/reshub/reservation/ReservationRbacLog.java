package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

final class ReservationRbacLog {

  static final String EVENT_READ_AUTHORIZED = "reservation.read.authorized";
  static final String EVENT_READ_FORBIDDEN = "reservation.read.forbidden";
  static final String EVENT_READ_NOT_FOUND = "reservation.read.not_found";
  static final String EVENT_LIST_AUTHORIZED = "reservation.list.authorized";
  static final String EVENT_CREATE_AUTHORIZED = "reservation.create.authorized";
  static final String EVENT_CREATE_FORBIDDEN = "reservation.create.forbidden";
  static final String EVENT_CREATE_CONFLICT = "reservation.create.conflict";
  static final String EVENT_CREATE_BAD_REQUEST = "reservation.create.bad_request";
  static final String EVENT_UPDATE_AUTHORIZED = "reservation.update.authorized";
  static final String EVENT_UPDATE_FORBIDDEN = "reservation.update.forbidden";
  static final String EVENT_UPDATE_NOT_FOUND = "reservation.update.not_found";
  static final String EVENT_COMMENT_AUTHORIZED = "reservation.comment.authorized";
  static final String EVENT_COMMENT_FORBIDDEN = "reservation.comment.forbidden";
  static final String EVENT_COMMENT_NOT_FOUND = "reservation.comment.not_found";
  static final String EVENT_STATUS_AUTHORIZED = "reservation.status.authorized";
  static final String EVENT_STATUS_FORBIDDEN = "reservation.status.forbidden";
  static final String EVENT_STATUS_NOT_FOUND = "reservation.status.not_found";
  static final String EVENT_STATUS_INVALID_TRANSITION = "reservation.status.invalid_transition";
  static final String EVENT_ADMIN_WRITE_AUDIT = "reservation.admin.write.audit";

  static final String REASON_HOTEL_SCOPE_MISMATCH = "hotel_scope_mismatch";
  static final String REASON_AGENCY_SCOPE_MISMATCH = "agency_scope_mismatch";
  static final String REASON_OWNER_MISMATCH = "owner_mismatch";
  static final String REASON_COMMENTS_INTERNAL_ONLY = "comments_internal_only";
  static final String REASON_INVALID_TRANSITION = "invalid_transition";
  static final String REASON_UNSUPPORTED_ROLE = "unsupported_role";

  private ReservationRbacLog() {
  }

  static String fields(String event, RequestActor actor, UUID reservationId, String decision, String reason) {
    String reservationValue = reservationId == null ? "n/a" : reservationId.toString();
    return "event=" + event +
      " actorUserId=" + actor.userId() +
      " actorRole=" + actor.role() +
      " actorHotelId=" + actor.hotelId() +
      " actorAgencyId=" + actor.agencyId() +
      " reservationId=" + reservationValue +
      " decision=" + decision +
      " reason=" + reason;
  }

  static String readFields(RequestActor actor, UUID reservationId, String decision, String reason) {
    String event = "ALLOW".equals(decision) ? EVENT_READ_AUTHORIZED : EVENT_READ_FORBIDDEN;
    return fields(event, actor, reservationId, decision, reason);
  }

  static String adminWriteAuditFields(RequestActor actor, UUID targetId, String action) {
    return "event=" + EVENT_ADMIN_WRITE_AUDIT +
      " actorUserId=" + actor.userId() +
      " actorRole=" + actor.role() +
      " action=" + action +
      " targetId=" + targetId +
      " occurredAt=" + OffsetDateTime.now(ZoneOffset.UTC);
  }
}
