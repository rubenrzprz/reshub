package com.rubenrzprz.reshub;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

class FlywayReservationsIT extends FlywayITBase {

  @Test
  void v4TablesConstraintsAndIndexesExist() {
    Assertions.assertEquals(1, tableExists("reservation"));
    Assertions.assertEquals(1, tableExists("reservation_comment"));
    Assertions.assertEquals(1, constraintExists("reservation_date_check"));
    Assertions.assertEquals(1, constraintExists("reservation_external_ref_unique"));
    Assertions.assertEquals(1, constraintExists("reservation_room_type_hotel_fk"));
    Assertions.assertEquals(1, indexExists("reservation", "idx_reservation_hotel_arrival"));
    Assertions.assertEquals(1, indexExists("reservation", "idx_reservation_hotel_status"));
  }

  @Test
  void v4HappyPathCreateAndFetch() {
    Seed seed = seedBaseline();
    UUID reservationId = UUID.randomUUID();
    String externalRef = "ext-" + reservationId.toString().substring(0, 8);
    jdbc.update(
      "insert into reservation " +
        "(id, hotel_id, agency_id, created_by_user_id, room_type_id, " +
        "external_room_type_code, external_room_type_name, external_ref, status, " +
        "arrival_date, departure_date, guest_name, guest_email, guest_phone, " +
        "adults, children, notes) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      reservationId,
      seed.hotelId,
      seed.agencyId,
      seed.userId,
      null,
      "EXT-STD",
      "Standard",
      externalRef,
      "NEW",
      Date.valueOf(LocalDate.of(2026, 2, 10)),
      Date.valueOf(LocalDate.of(2026, 2, 12)),
      "Ada Lovelace",
      "ada@example.com",
      "+1-555-0100",
      2,
      0,
      "High floor"
    );

    String storedRef = jdbc.queryForObject(
      "select external_ref from reservation where id = ?",
      String.class,
      reservationId
    );
    Assertions.assertEquals(externalRef, storedRef);
  }

  @Test
  void v4IdempotencyRejectsDuplicateExternalRef() {
    Seed seed = seedBaseline();
    String externalRef = "ext-dup-" + seed.hotelId.toString().substring(0, 6);
    insertReservation(seed, UUID.randomUUID(), externalRef, "NEW",
      LocalDate.of(2026, 2, 14), LocalDate.of(2026, 2, 15));

    Assertions.assertThrows(DataIntegrityViolationException.class, () ->
      insertReservation(seed, UUID.randomUUID(), externalRef, "NEW",
        LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 18))
    );
  }

  @Test
  void v4InvalidDatesOrTransitionsAreRejected() {
    Seed seed = seedBaseline();
    Assertions.assertThrows(DataIntegrityViolationException.class, () ->
      insertReservation(seed, UUID.randomUUID(), "ext-bad-date", "NEW",
        LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 20))
    );

    UUID reservationId = UUID.randomUUID();
    insertReservation(seed, reservationId, "ext-cancelled", "CANCELLED",
      LocalDate.of(2026, 2, 22), LocalDate.of(2026, 2, 23));
    Assertions.assertThrows(DataAccessException.class, () ->
      jdbc.update(
        "update reservation set status = ? where id = ?",
        "CONFIRMED",
        reservationId
      )
    );
  }

  @Test
  void v4CommentInsertOk() {
    Seed seed = seedBaseline();
    UUID reservationId = UUID.randomUUID();
    insertReservation(seed, reservationId, "ext-comment", "NEW",
      LocalDate.of(2026, 2, 25), LocalDate.of(2026, 2, 26));

    UUID commentId = UUID.randomUUID();
    jdbc.update(
      "insert into reservation_comment (id, reservation_id, author_user_id, body) " +
        "values (?, ?, ?, ?)",
      commentId,
      reservationId,
      seed.userId,
      "Internal note"
    );

    Integer count = jdbc.queryForObject(
      "select count(*) from reservation_comment where id = ?",
      Integer.class,
      commentId
    );
    Assertions.assertEquals(1, count);
  }

  private void insertReservation(
    Seed seed,
    UUID reservationId,
    String externalRef,
    String status,
    LocalDate arrival,
    LocalDate departure
  ) {
    jdbc.update(
      "insert into reservation " +
        "(id, hotel_id, agency_id, created_by_user_id, external_ref, status, " +
        "arrival_date, departure_date, guest_name, adults, children) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      reservationId,
      seed.hotelId,
      seed.agencyId,
      seed.userId,
      externalRef,
      status,
      Date.valueOf(arrival),
      Date.valueOf(departure),
      "Test Guest",
      1,
      0
    );
  }

  private Seed seedBaseline() {
    UUID hotelId = UUID.randomUUID();
    String hotelCode = "h" + hotelId.toString().replace("-", "").substring(0, 8);
    jdbc.update(
      "insert into hotel (id, code, name) values (?, ?, ?)",
      hotelId,
      hotelCode,
      "Test Hotel"
    );

    UUID agencyId = UUID.randomUUID();
    String agencyCode = "a" + agencyId.toString().replace("-", "").substring(0, 8);
    jdbc.update(
      "insert into agency (id, code, name) values (?, ?, ?)",
      agencyId,
      agencyCode,
      "Test Agency"
    );

    UUID userId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      userId,
      "user-" + userId.toString().substring(0, 8) + "@example.com",
      "hash",
      "MANAGER",
      hotelId
    );

    return new Seed(hotelId, agencyId, userId);
  }

  private record Seed(UUID hotelId, UUID agencyId, UUID userId) {}
}
