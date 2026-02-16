package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationCommandService {

  private static final Logger log = LoggerFactory.getLogger(ReservationCommandService.class);

  private final JdbcTemplate jdbc;
  private final ReservationQueryService reservationQueryService;

  public ReservationCommandService(JdbcTemplate jdbc, ReservationQueryService reservationQueryService) {
    this.jdbc = jdbc;
    this.reservationQueryService = reservationQueryService;
  }

  public ReservationView updateNotes(UUID reservationId, String notes, RequestActor actor) {
    ReservationScope scope = loadScope(reservationId, actor);
    enforceUpdateAccess(actor, scope);

    jdbc.update("update reservation set notes = ?, updated_at = now() where id = ?", notes, reservationId);

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
    return reservationQueryService.getById(reservationId, actor);
  }

  public ReservationCommentView addComment(UUID reservationId, String body, RequestActor actor) {
    if (body == null || body.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comment body is required");
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
      "select id, hotel_id, agency_id, created_by_user_id from reservation where id = ?",
      (rs, rowNum) -> new ReservationScope(
        UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("hotel_id")),
        UUID.fromString(rs.getString("agency_id")),
        UUID.fromString(rs.getString("created_by_user_id"))
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
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation not found");
    }

    return rows.getFirst();
  }

  private ReservationScope loadScope(UUID reservationId, RequestActor actor) {
    return loadScope(reservationId, actor, ReservationRbacLog.EVENT_UPDATE_NOT_FOUND);
  }

  private void enforceUpdateAccess(RequestActor actor, ReservationScope scope) {
    Consumer<String> onDeny = reason -> deny(actor, scope.id(), reason);
    switch (actor.role()) {
      case MANAGER -> ReservationRbacGuards.requireHotelScope(actor, scope.hotelId(), onDeny);
      case RECEPTIONIST -> {
        ReservationRbacGuards.requireHotelScope(actor, scope.hotelId(), onDeny);
        ReservationRbacGuards.requireOwner(actor, scope.createdByUserId(), onDeny);
      }
      case AGENCY -> ReservationRbacGuards.requireAgencyScope(actor, scope.agencyId(), onDeny);
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void enforceCommentAccess(RequestActor actor, ReservationScope scope) {
    Consumer<String> onDeny = reason -> deny(actor, scope.id(), ReservationRbacLog.EVENT_COMMENT_FORBIDDEN, reason);
    switch (actor.role()) {
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
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden reservation scope");
  }

  private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
  }

  private record ReservationScope(UUID id, UUID hotelId, UUID agencyId, UUID createdByUserId) {
  }
}
