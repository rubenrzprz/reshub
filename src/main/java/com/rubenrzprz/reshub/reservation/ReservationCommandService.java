package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.api.ApiProblemException;
import com.rubenrzprz.reshub.security.RequestActor;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReservationCommandService {

  private static final Logger log = LoggerFactory.getLogger(ReservationCommandService.class);

  private final JdbcTemplate jdbc;
  private final ReservationQueryService reservationQueryService;
  private final AgencyHotelAuthorizationService agencyHotelAuthorizationService;

  public ReservationCommandService(
    JdbcTemplate jdbc,
    ReservationQueryService reservationQueryService,
    AgencyHotelAuthorizationService agencyHotelAuthorizationService
  ) {
    this.jdbc = jdbc;
    this.reservationQueryService = reservationQueryService;
    this.agencyHotelAuthorizationService = agencyHotelAuthorizationService;
  }

  public ReservationView updateNotes(UUID reservationId, String notes, RequestActor actor) {
    ReservationScope scope = loadScope(reservationId, actor);
    enforceMutationAccess(actor, scope, ReservationRbacLog.EVENT_UPDATE_FORBIDDEN);

    try {
      jdbc.update("update reservation set notes = ?, updated_at = now() where id = ?", notes, reservationId);
    } catch (DataAccessException ex) {
      throw mapMutationViolation(ex);
    }

    log.debug(
      "{}",
      ReservationRbacLog.fields(
        ReservationRbacLog.EVENT_UPDATE_AUTHORIZED,
        actor,
        reservationId,
        "ALLOW",
        "scope_match"
      )
    );
    emitAdminWriteAudit(actor, reservationId, "update_notes");
    return reservationQueryService.getById(reservationId, actor);
  }

  public ReservationView createReservation(ReservationCreateRequest request, RequestActor actor) {
    validateCreateRequest(request);
    enforceCreateAccess(actor, request);

    UUID reservationId = UUID.randomUUID();
    int adults = request.adults() == null ? 1 : request.adults();
    int children = request.children() == null ? 0 : request.children();

    try {
      jdbc.update(
        "insert into reservation " +
          "(id, hotel_id, agency_id, created_by_user_id, room_type_id, external_room_type_code, external_room_type_name, " +
          "external_ref, status, arrival_date, departure_date, guest_name, adults, children, notes) " +
          "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        reservationId,
        request.hotelId(),
        request.agencyId(),
        actor.userId(),
        request.roomTypeId(),
        request.externalRoomTypeCode(),
        request.externalRoomTypeName(),
        request.externalRef(),
        "NEW",
        java.sql.Date.valueOf(request.arrivalDate()),
        java.sql.Date.valueOf(request.departureDate()),
        request.guestName(),
        adults,
        children,
        request.notes()
      );
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
      if (message.contains("reservation_external_ref_unique")) {
        log.warn(
          "event={} actorUserId={} actorRole={} actorHotelId={} actorAgencyId={} reason=duplicate_external_ref",
          ReservationRbacLog.EVENT_CREATE_CONFLICT,
          actor.userId(),
          actor.role(),
          actor.hotelId(),
          actor.agencyId()
        );
        throw new ApiProblemException(
          HttpStatus.CONFLICT,
          "duplicate_external_ref",
          "duplicate external reference for hotel and agency"
        );
      }

      log.warn(
        "event={} actorUserId={} actorRole={} actorHotelId={} actorAgencyId={} reason=data_integrity_violation",
        ReservationRbacLog.EVENT_CREATE_BAD_REQUEST,
        actor.userId(),
        actor.role(),
        actor.hotelId(),
        actor.agencyId()
      );
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_reservation_payload", "invalid reservation payload");
    }

    log.debug(
      "{}",
      ReservationRbacLog.fields(
        ReservationRbacLog.EVENT_CREATE_AUTHORIZED,
        actor,
        reservationId,
        "ALLOW",
        "scope_match"
      )
    );
    emitAdminWriteAudit(actor, reservationId, "create_reservation");
    return reservationQueryService.getById(reservationId, actor);
  }

  public ReservationView confirm(UUID reservationId, RequestActor actor) {
    return changeStatus(reservationId, actor, "CONFIRMED", null);
  }

  public ReservationView cancel(UUID reservationId, String reason, RequestActor actor) {
    return changeStatus(reservationId, actor, "CANCELLED", reason);
  }

  public ReservationView noShow(UUID reservationId, RequestActor actor) {
    return changeStatus(reservationId, actor, "NOSHOW", null);
  }

  public ReservationCommentView addComment(UUID reservationId, String body, RequestActor actor) {
    if (body == null || body.isBlank()) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_comment_payload", "comment body is required");
    }

    ReservationScope scope = loadScope(reservationId, actor, ReservationRbacLog.EVENT_COMMENT_NOT_FOUND);
    enforceCommentAccess(actor, scope);

    UUID commentId = UUID.randomUUID();
    jdbc.update(
      "insert into reservation_comment (id, reservation_id, author_user_id, body) values (?, ?, ?, ?)",
      commentId,
      reservationId,
      actor.userId(),
      body
    );

    log.debug(
      "{}",
      ReservationRbacLog.fields(
        ReservationRbacLog.EVENT_COMMENT_AUTHORIZED,
        actor,
        reservationId,
        "ALLOW",
        "scope_match"
      )
    );
    emitAdminWriteAudit(actor, reservationId, "create_comment");

    return jdbc.queryForObject(
      "select id, reservation_id, author_user_id, body, created_at from reservation_comment where id = ?",
      (rs, rowNum) -> new ReservationCommentView(
        UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("reservation_id")),
        UUID.fromString(rs.getString("author_user_id")),
        rs.getString("body"),
        toOffsetDateTime(rs.getTimestamp("created_at"))
      ),
      commentId
    );
  }

  private ReservationScope loadScope(UUID reservationId, RequestActor actor, String notFoundEvent) {
    List<ReservationScope> rows = jdbc.query(
      "select id, hotel_id, agency_id, created_by_user_id, status from reservation where id = ?",
      (rs, rowNum) -> new ReservationScope(
        UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("hotel_id")),
        UUID.fromString(rs.getString("agency_id")),
        UUID.fromString(rs.getString("created_by_user_id")),
        rs.getString("status")
      ),
      reservationId
    );

    if (rows.isEmpty()) {
      log.warn(
        "event={} actorUserId={} actorRole={} actorHotelId={} actorAgencyId={} reservationId={} reason=not_found",
        notFoundEvent,
        actor.userId(),
        actor.role(),
        actor.hotelId(),
        actor.agencyId(),
        reservationId
      );
      throw new ApiProblemException(HttpStatus.NOT_FOUND, "reservation_not_found", "reservation not found");
    }

    return rows.getFirst();
  }

  private ReservationScope loadScope(UUID reservationId, RequestActor actor) {
    return loadScope(reservationId, actor, ReservationRbacLog.EVENT_UPDATE_NOT_FOUND);
  }

  private void enforceMutationAccess(RequestActor actor, ReservationScope scope, String forbiddenEvent) {
    Consumer<String> onDeny = reason -> deny(actor, scope.id(), forbiddenEvent, reason);
    switch (actor.role()) {
      case ADMIN -> {
      }
      case MANAGER -> ReservationRbacGuards.requireHotelScope(actor, scope.hotelId(), onDeny);
      case RECEPTIONIST -> {
        ReservationRbacGuards.requireHotelScope(actor, scope.hotelId(), onDeny);
        ReservationRbacGuards.requireOwner(actor, scope.createdByUserId(), onDeny);
      }
      case AGENCY -> ReservationRbacGuards.requireAgencyScope(actor, scope.agencyId(), onDeny);
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void enforceCreateAccess(RequestActor actor, ReservationCreateRequest request) {
    Consumer<String> onDeny = reason -> deny(actor, null, ReservationRbacLog.EVENT_CREATE_FORBIDDEN, reason);
    switch (actor.role()) {
      case ADMIN -> {
      }
      case MANAGER, RECEPTIONIST -> ReservationRbacGuards.requireHotelScope(actor, request.hotelId(), onDeny);
      case AGENCY -> {
        ReservationRbacGuards.requireAgencyScope(actor, request.agencyId(), onDeny);
        enforceAgencyHotelAuthorization(actor, request.hotelId(), request.arrivalDate(), onDeny);
      }
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void enforceAgencyHotelAuthorization(
    RequestActor actor,
    UUID hotelId,
    LocalDate arrivalDate,
    Consumer<String> onDeny
  ) {
    if (!agencyHotelAuthorizationService.isAgencyAuthorizedForHotelOnDate(actor, hotelId, arrivalDate)) {
      onDeny.accept(ReservationRbacLog.REASON_AGENCY_HOTEL_AUTH_MISSING_OR_INVALID);
    }
  }

  private void validateCreateRequest(ReservationCreateRequest request) {
    if (request == null ||
      request.hotelId() == null ||
      request.agencyId() == null ||
      request.externalRef() == null || request.externalRef().isBlank() ||
      request.arrivalDate() == null ||
      request.departureDate() == null ||
      request.guestName() == null || request.guestName().isBlank()) {
      throw new ApiProblemException(
        HttpStatus.BAD_REQUEST,
        "invalid_reservation_payload",
        "missing required reservation fields"
      );
    }

    if (!request.arrivalDate().isBefore(request.departureDate())) {
      throw new ApiProblemException(
        HttpStatus.BAD_REQUEST,
        "invalid_reservation_payload",
        "arrival date must be before departure date"
      );
    }

    if (request.adults() != null && request.adults() < 1) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_reservation_payload", "adults must be at least 1");
    }

    if (request.children() != null && request.children() < 0) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_reservation_payload", "children cannot be negative");
    }
  }

  private void enforceCommentAccess(RequestActor actor, ReservationScope scope) {
    Consumer<String> onDeny = reason -> deny(actor, scope.id(), ReservationRbacLog.EVENT_COMMENT_FORBIDDEN, reason);
    switch (actor.role()) {
      case ADMIN -> {
      }
      case MANAGER, RECEPTIONIST -> ReservationRbacGuards.requireHotelScope(actor, scope.hotelId(), onDeny);
      case AGENCY -> onDeny.accept(ReservationRbacLog.REASON_COMMENTS_INTERNAL_ONLY);
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void deny(RequestActor actor, UUID reservationId, String reason) {
    deny(actor, reservationId, ReservationRbacLog.EVENT_UPDATE_FORBIDDEN, reason);
  }

  private void deny(RequestActor actor, UUID reservationId, String event, String reason) {
    log.warn(
      "{}",
      ReservationRbacLog.fields(
        event,
        actor,
        reservationId,
        "DENY",
        reason
      )
    );
    if (ReservationRbacLog.REASON_AGENCY_HOTEL_AUTH_MISSING_OR_INVALID.equals(reason)) {
      throw new ApiProblemException(
        HttpStatus.FORBIDDEN,
        "agency_not_authorized_for_hotel",
        "agency is not authorized for hotel"
      );
    }
    throw new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden_scope", "forbidden reservation scope");
  }

  private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
  }

  private ReservationView changeStatus(UUID reservationId, RequestActor actor, String targetStatus, String cancelReason) {
    ReservationScope scope = loadScope(reservationId, actor, ReservationRbacLog.EVENT_STATUS_NOT_FOUND);
    enforceMutationAccess(actor, scope, ReservationRbacLog.EVENT_STATUS_FORBIDDEN);

    if (!isAllowedTransition(scope.status(), targetStatus)) {
      log.warn(
        "{}",
        ReservationRbacLog.fields(
          ReservationRbacLog.EVENT_STATUS_INVALID_TRANSITION,
          actor,
          reservationId,
          "DENY",
          ReservationRbacLog.REASON_INVALID_TRANSITION + ":" + scope.status() + "_to_" + targetStatus
        )
      );
      throw new ApiProblemException(
        HttpStatus.CONFLICT,
        "invalid_status_transition",
        "invalid reservation status transition"
      );
    }

    try {
      if ("CANCELLED".equals(targetStatus)) {
        jdbc.update(
          "update reservation set status = ?, cancelled_at = now(), cancel_reason = ?, updated_at = now() where id = ?",
          targetStatus,
          cancelReason,
          reservationId
        );
      } else {
        jdbc.update(
          "update reservation set status = ?, cancelled_at = null, cancel_reason = null, updated_at = now() where id = ?",
          targetStatus,
          reservationId
        );
      }
    } catch (DataAccessException ex) {
      throw mapMutationViolation(ex);
    }

    log.debug(
      "{}",
      ReservationRbacLog.fields(
        ReservationRbacLog.EVENT_STATUS_AUTHORIZED,
        actor,
        reservationId,
        "ALLOW",
        "transition:" + scope.status() + "_to_" + targetStatus
      )
    );
    emitAdminWriteAudit(actor, reservationId, "change_status_to_" + targetStatus);
    return reservationQueryService.getById(reservationId, actor);
  }

  private boolean isAllowedTransition(String currentStatus, String targetStatus) {
    if ("NEW".equals(currentStatus)) {
      return "CONFIRMED".equals(targetStatus) || "CANCELLED".equals(targetStatus) || "NOSHOW".equals(targetStatus);
    }
    if ("CONFIRMED".equals(currentStatus)) {
      return "CANCELLED".equals(targetStatus) || "NOSHOW".equals(targetStatus);
    }
    return false;
  }

  private ApiProblemException mapMutationViolation(DataAccessException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    if (message.contains("invalid reservation status transition") ||
      message.contains("terminal reservation is immutable")) {
      return new ApiProblemException(
        HttpStatus.CONFLICT,
        "invalid_status_transition",
        "invalid reservation status transition"
      );
    }
    return new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_reservation_payload", "invalid reservation payload");
  }

  private void emitAdminWriteAudit(RequestActor actor, UUID targetId, String action) {
    if (actor.role() == RequestActor.Role.ADMIN) {
      log.info("{}", ReservationRbacLog.adminWriteAuditFields(actor, targetId, action));
    }
  }

  private record ReservationScope(UUID id, UUID hotelId, UUID agencyId, UUID createdByUserId, String status) {
  }
}
