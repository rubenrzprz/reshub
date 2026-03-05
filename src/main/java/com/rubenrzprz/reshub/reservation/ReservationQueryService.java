package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.api.ApiProblemException;
import com.rubenrzprz.reshub.security.RequestActor;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReservationQueryService {

  private static final Logger log = LoggerFactory.getLogger(ReservationQueryService.class);

  private final JdbcTemplate jdbc;
  private final AgencyHotelAuthorizationService agencyHotelAuthorizationService;

  public ReservationQueryService(
    JdbcTemplate jdbc,
    AgencyHotelAuthorizationService agencyHotelAuthorizationService
  ) {
    this.jdbc = jdbc;
    this.agencyHotelAuthorizationService = agencyHotelAuthorizationService;
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
      throw new ApiProblemException(HttpStatus.NOT_FOUND, "reservation_not_found", "reservation not found");
    }

    ReservationView reservation = rows.getFirst();
    enforceReadAccess(actor, reservation);
    log.debug(
      "{}",
      ReservationRbacLog.readFields(actor, reservation.id(), "ALLOW", "scope_match")
    );
    return reservation;
  }

  public ReservationListResponse list(
    RequestActor actor,
    int limit,
    String cursor,
    String status,
    LocalDate arrivalFrom,
    LocalDate arrivalTo,
    String guestQuery
  ) {
    CursorKey cursorKey = parseCursor(cursor);

    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder(
      "select id, hotel_id, agency_id, created_by_user_id, status, arrival_date, departure_date, guest_name, notes " +
        "from reservation where "
    );

    appendScopeAndFilters(sql, args, actor, status, arrivalFrom, arrivalTo, guestQuery);

    if (cursorKey != null) {
      sql.append("and (arrival_date > ? or (arrival_date = ? and id > ?)) ");
      args.add(Date.valueOf(cursorKey.arrivalDate()));
      args.add(Date.valueOf(cursorKey.arrivalDate()));
      args.add(cursorKey.id());
    }

    sql.append("order by arrival_date asc, id asc limit ?");
    args.add(limit + 1);

    List<ReservationView> rows = jdbc.query(
      sql.toString(),
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
      args.toArray()
    );

    String nextCursor = null;
    if (rows.size() > limit) {
      ReservationView last = rows.get(limit - 1);
      nextCursor = encodeCursor(new CursorKey(last.arrivalDate(), last.id()));
      rows = rows.subList(0, limit);
    }

    log.debug(
      "event={} actorUserId={} actorRole={} actorHotelId={} actorAgencyId={} decision=ALLOW reason=scope_query nextCursorPresent={} resultCount={}",
      ReservationRbacLog.EVENT_LIST_AUTHORIZED,
      actor.userId(),
      actor.role(),
      actor.hotelId(),
      actor.agencyId(),
      nextCursor != null,
      rows.size()
    );

    return new ReservationListResponse(rows, nextCursor, limit);
  }

  public List<ReservationView> export(
    RequestActor actor,
    String status,
    LocalDate arrivalFrom,
    LocalDate arrivalTo,
    String guestQuery
  ) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder(
      "select id, hotel_id, agency_id, created_by_user_id, status, arrival_date, departure_date, guest_name, notes " +
        "from reservation where "
    );

    appendScopeAndFilters(sql, args, actor, status, arrivalFrom, arrivalTo, guestQuery);
    sql.append("order by arrival_date asc, id asc");

    return jdbc.query(
      sql.toString(),
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
      args.toArray()
    );
  }

  private void appendScopeAndFilters(
    StringBuilder sql,
    List<Object> args,
    RequestActor actor,
    String status,
    LocalDate arrivalFrom,
    LocalDate arrivalTo,
    String guestQuery
  ) {
    switch (actor.role()) {
      case ADMIN -> sql.append("1 = 1 ");
      case MANAGER, RECEPTIONIST -> {
        sql.append("hotel_id = ? ");
        args.add(actor.hotelId());
      }
      case AGENCY -> {
        sql.append("agency_id = ? ");
        args.add(actor.agencyId());
        appendAgencyHotelAuthorizationFilter(sql, args, actor);
      }
      default -> throw new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden_scope", "forbidden reservation scope");
    }

    if (status != null && !status.isBlank()) {
      sql.append("and status = ? ");
      args.add(status);
    }

    if (arrivalFrom != null) {
      sql.append("and arrival_date >= ? ");
      args.add(Date.valueOf(arrivalFrom));
    }

    if (arrivalTo != null) {
      sql.append("and arrival_date <= ? ");
      args.add(Date.valueOf(arrivalTo));
    }

    if (guestQuery != null && !guestQuery.isBlank()) {
      String guestPattern = "%" + guestQuery.trim().toLowerCase() + "%";
      sql.append("and (");
      sql.append("lower(guest_name) like ? ");
      sql.append("or lower(coalesce(guest_email, '')) like ? ");
      sql.append("or lower(coalesce(guest_phone, '')) like ? ");
      sql.append(") ");
      args.add(guestPattern);
      args.add(guestPattern);
      args.add(guestPattern);
    }
  }

  private void appendAgencyHotelAuthorizationFilter(
    StringBuilder sql,
    List<Object> args,
    RequestActor actor
  ) {
    if (!agencyHotelAuthorizationService.isAgencyAuthEnforced(actor)) {
      return;
    }
    sql.append("and exists (");
    sql.append("select 1 from agency_hotel_auth aha ");
    sql.append("where aha.agency_id = ? ");
    sql.append("and aha.hotel_id = reservation.hotel_id ");
    sql.append("and aha.status = 'ACTIVE' ");
    sql.append("and (aha.valid_from is null or aha.valid_from <= reservation.arrival_date) ");
    sql.append("and (aha.valid_to is null or aha.valid_to >= reservation.arrival_date)");
    sql.append(") ");
    args.add(actor.agencyId());
  }

  private void enforceReadAccess(RequestActor actor, ReservationView reservation) {
    Consumer<String> onDeny = reason -> deny(actor, reservation.id(), reason);
    switch (actor.role()) {
      case ADMIN -> {
      }
      case MANAGER, RECEPTIONIST -> ReservationRbacGuards.requireHotelScope(actor, reservation.hotelId(), onDeny);
      case AGENCY -> {
        ReservationRbacGuards.requireAgencyScope(actor, reservation.agencyId(), onDeny);
        enforceAgencyHotelAuthorization(actor, reservation, onDeny);
      }
      default -> onDeny.accept(ReservationRbacLog.REASON_UNSUPPORTED_ROLE);
    }
  }

  private void enforceAgencyHotelAuthorization(
    RequestActor actor,
    ReservationView reservation,
    Consumer<String> onDeny
  ) {
    if (!agencyHotelAuthorizationService.isAgencyAuthorizedForHotelOnDate(
      actor,
      reservation.hotelId(),
      reservation.arrivalDate()
    )) {
      onDeny.accept(ReservationRbacLog.REASON_AGENCY_HOTEL_AUTH_MISSING_OR_INVALID);
    }
  }

  private void deny(RequestActor actor, UUID reservationId, String reason) {
    log.warn("{}", ReservationRbacLog.readFields(actor, reservationId, "DENY", reason));
    if (ReservationRbacLog.REASON_AGENCY_HOTEL_AUTH_MISSING_OR_INVALID.equals(reason)) {
      throw new ApiProblemException(
        HttpStatus.FORBIDDEN,
        "agency_not_authorized_for_hotel",
        "agency is not authorized for hotel"
      );
    }
    throw new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden_scope", "forbidden reservation scope");
  }

  private java.time.LocalDate toLocalDate(Date value) {
    return value == null ? null : value.toLocalDate();
  }

  private CursorKey parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      String[] parts = decoded.split("\\|", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("invalid cursor format");
      }
      return new CursorKey(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
    } catch (Exception ex) {
      throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_cursor", "invalid cursor");
    }
  }

  private String encodeCursor(CursorKey key) {
    String raw = key.arrivalDate() + "|" + key.id();
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private record CursorKey(LocalDate arrivalDate, UUID id) {
  }
}
