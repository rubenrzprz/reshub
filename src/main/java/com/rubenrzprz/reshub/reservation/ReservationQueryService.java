package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.sql.Date;
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
public class ReservationQueryService {

  private static final Logger log = LoggerFactory.getLogger(ReservationQueryService.class);

  private final JdbcTemplate jdbc;

  public ReservationQueryService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public ReservationView getById(UUID reservationId, RequestActor actor) {
    List<ReservationView> rows = jdbc.query(
      "select id, hotel_id, agency_id, created_by_user_id, status, arrival_date, departure_date, guest_name, notes " +
        "from reservation where id = ?",
      (rs, rowNum) -> new ReservationView(
        UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("hotel_id")),
        UUID.fromString(rs.getString("agency_id")),
        UUID.fromString(rs.getString("created_by_user_id")),
        rs.getString("status"),
        toLocalDate(rs.getDate("arrival_date")),
        toLocalDate(rs.getDate("departure_date")),
        rs.getString("guest_name"),
        rs.getString("notes")
      ),
      reservationId
    );

    if (rows.isEmpty()) {
      log.warn(
        "event={} actorUserId={} actorRole={} actorHotelId={} actorAgencyId={} reservationId={} reason=not_found",
        ReservationRbacLog.EVENT_READ_NOT_FOUND,
        actor.userId(),
        actor.role(),
        actor.hotelId(),
        actor.agencyId(),
        reservationId
      );
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation not found");
    }

    ReservationView reservation = rows.getFirst();
    enforceReadAccess(actor, reservation);
    log.debug(
      "{}",
      ReservationRbacLog.readFields(actor, reservation.id(), "ALLOW", "scope_match")
    );
    return reservation;
  }

  private void enforceReadAccess(RequestActor actor, ReservationView reservation) {
    Consumer<String> onDeny = reason -> deny(actor, reservation.id(), reason);
    switch (actor.role()) {
      case MANAGER, RECEPTIONIST -> ReservationRbacGuards.requireHotelScope(actor, reservation.hotelId(), onDeny);
      case AGENCY -> ReservationRbacGuards.requireAgencyScope(actor, reservation.agencyId(), onDeny);
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void deny(RequestActor actor, UUID reservationId, String reason) {
    log.warn("{}", ReservationRbacLog.readFields(actor, reservationId, "DENY", reason));
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden reservation scope");
  }

  private java.time.LocalDate toLocalDate(Date value) {
    return value == null ? null : value.toLocalDate();
  }
}
