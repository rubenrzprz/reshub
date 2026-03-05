package com.rubenrzprz.reshub.reservation.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

public class ReservationTestDataFactory {

  private final JdbcTemplate jdbc;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public ReservationTestDataFactory(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void truncateAll() {
    jdbc.execute("truncate table reservation_comment, reservation, room_type_channel_map, room_type, app_user, agency, hotel restart identity cascade");
  }

  public Seed seedBaseline(String validPassword) {
    UUID hotelId = UUID.randomUUID();
    jdbc.update("insert into hotel (id, code, name) values (?, ?, ?)",
      hotelId,
      "h" + hotelId.toString().replace("-", "").substring(0, 8),
      "Test Hotel");

    UUID agencyId = UUID.randomUUID();
    jdbc.update("insert into agency (id, code, name) values (?, ?, ?)",
      agencyId,
      "a" + agencyId.toString().replace("-", "").substring(0, 8),
      "Test Agency");

    UUID managerUserId = UUID.randomUUID();
    String managerEmail = "manager-" + managerUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      managerUserId,
      managerEmail,
      encoder.encode(validPassword),
      "MANAGER",
      hotelId
    );

    UUID adminUserId = UUID.randomUUID();
    String adminEmail = "admin-" + adminUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      adminUserId,
      adminEmail,
      encoder.encode(validPassword),
      "ADMIN",
      hotelId
    );

    UUID receptionistUserId = UUID.randomUUID();
    String receptionistEmail = "reception-" + receptionistUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      receptionistUserId,
      receptionistEmail,
      encoder.encode(validPassword),
      "RECEPTIONIST",
      hotelId
    );

    UUID agencyUserId = UUID.randomUUID();
    String agencyEmail = "agency-" + agencyUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, agency_id) values (?, ?, ?, ?, ?)",
      agencyUserId,
      agencyEmail,
      encoder.encode(validPassword),
      "AGENCY",
      agencyId
    );

    UUID otherHotelId = UUID.randomUUID();
    jdbc.update("insert into hotel (id, code, name) values (?, ?, ?)",
      otherHotelId,
      "h" + otherHotelId.toString().replace("-", "").substring(0, 8),
      "Other Hotel");

    UUID otherAgencyId = UUID.randomUUID();
    jdbc.update("insert into agency (id, code, name) values (?, ?, ?)",
      otherAgencyId,
      "a" + otherAgencyId.toString().replace("-", "").substring(0, 8),
      "Other Agency");

    UUID otherManagerUserId = UUID.randomUUID();
    String otherManagerEmail = "other-manager-" + otherManagerUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      otherManagerUserId,
      otherManagerEmail,
      encoder.encode(validPassword),
      "MANAGER",
      otherHotelId
    );

    UUID otherAgencyUserId = UUID.randomUUID();
    String otherAgencyEmail = "other-agency-" + otherAgencyUserId.toString().substring(0, 8) + "@example.com";
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, agency_id) values (?, ?, ?, ?, ?)",
      otherAgencyUserId,
      otherAgencyEmail,
      encoder.encode(validPassword),
      "AGENCY",
      otherAgencyId
    );

    return new Seed(
      hotelId,
      agencyId,
      adminUserId,
      adminEmail,
      managerUserId,
      managerEmail,
      receptionistUserId,
      receptionistEmail,
      agencyUserId,
      agencyEmail,
      otherHotelId,
      otherAgencyId,
      otherManagerUserId,
      otherManagerEmail,
      otherAgencyUserId,
      otherAgencyEmail
    );
  }

  public UUID insertReservation(Seed s) {
    return insertReservation(s, s.receptionistUserId());
  }

  public UUID insertReservationWithGuest(
    Seed s,
    UUID hotelId,
    UUID agencyId,
    UUID createdByUserId,
    String status,
    LocalDate arrivalDate,
    String guestName,
    String guestEmail,
    String guestPhone
  ) {
    UUID reservationIdWithGuest = insertReservation(s, hotelId, agencyId, createdByUserId, status, arrivalDate);
    jdbc.update(
      "update reservation set guest_name = ?, guest_email = ?, guest_phone = ? where id = ?",
      guestName,
      guestEmail,
      guestPhone,
      reservationIdWithGuest
    );
    return reservationIdWithGuest;
  }

  public UUID insertReservation(Seed s, UUID createdByUserId) {
    return insertReservation(s, s.hotelId(), s.agencyId(), createdByUserId, "NEW", LocalDate.of(2026, 2, 20));
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
      "Test Guest",
      2,
      0
    );
    return id;
  }

  public record Seed(
    UUID hotelId,
    UUID agencyId,
    UUID adminUserId,
    String adminEmail,
    UUID managerUserId,
    String managerEmail,
    UUID receptionistUserId,
    String receptionistEmail,
    UUID agencyUserId,
    String agencyEmail,
    UUID otherHotelId,
    UUID otherAgencyId,
    UUID otherManagerUserId,
    String otherManagerEmail,
    UUID otherAgencyUserId,
    String otherAgencyEmail
  ) {
  }

}
