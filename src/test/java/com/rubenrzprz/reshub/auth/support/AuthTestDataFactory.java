package com.rubenrzprz.reshub.auth.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

public class AuthTestDataFactory {

  private final JdbcTemplate jdbc;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthTestDataFactory(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void truncateAll() {
    jdbc.execute("truncate table reservation_comment, reservation, room_type_channel_map, room_type, app_user, agency, hotel restart identity cascade");
  }

  public UUID insertReservation(
    Seed s,
    UUID hotelId,
    UUID agencyId,
    UUID createdByUserId,
    String status,
    LocalDate arrivalDate
  ) {
    UUID id = UUID.randomUUID();
    jdbc.update(
      "insert into reservation " +
        "(id, hotel_id, agency_id, created_by_user_id, external_ref, status, arrival_date, departure_date, guest_name, adults, children) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      id,
      hotelId,
      agencyId,
      createdByUserId,
      "ext-" + id.toString().substring(0, 8),
      status,
      java.sql.Date.valueOf(arrivalDate),
      java.sql.Date.valueOf(arrivalDate.plusDays(2)),
      "Auth Guest",
      2,
      0
    );
    return id;
  }

  public Seed seedBaseline(String password) {
    UUID hotelId = UUID.randomUUID();
    jdbc.update("insert into hotel (id, code, name) values (?, ?, ?)",
      hotelId,
      "h" + hotelId.toString().replace("-", "").substring(0, 8),
      "Auth Test Hotel");

    UUID otherHotelId = UUID.randomUUID();
    jdbc.update("insert into hotel (id, code, name) values (?, ?, ?)",
      otherHotelId,
      "h" + otherHotelId.toString().replace("-", "").substring(0, 8),
      "Auth Other Hotel");

    UUID agencyId = UUID.randomUUID();
    jdbc.update("insert into agency (id, code, name) values (?, ?, ?)",
      agencyId,
      "a" + agencyId.toString().replace("-", "").substring(0, 8),
      "Auth Test Agency");

    UUID managerUserId = UUID.randomUUID();
    String managerEmail = "manager-" + managerUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      managerUserId,
      managerEmail,
      encoder.encode(password),
      "MANAGER",
      hotelId
    );

    UUID otherManagerUserId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      otherManagerUserId,
      "other-manager-" + otherManagerUserId.toString().substring(0, 8) + "@example.com",
      encoder.encode(password),
      "MANAGER",
      otherHotelId
    );

    return new Seed(
      hotelId,
      otherHotelId,
      agencyId,
      managerUserId,
      managerEmail,
      otherManagerUserId
    );
  }

  public UUID insertManagerUser(UUID hotelId, String email, String password) {
    UUID userId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      userId,
      email,
      encoder.encode(password),
      "MANAGER",
      hotelId
    );
    return userId;
  }

  public record Seed(
    UUID hotelId,
    UUID otherHotelId,
    UUID agencyId,
    UUID managerUserId,
    String managerEmail,
    UUID otherManagerUserId
  ) {
  }

}
