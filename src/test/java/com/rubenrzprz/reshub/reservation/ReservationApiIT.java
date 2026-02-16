package com.rubenrzprz.reshub.reservation;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationApiIT {

  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("reshub")
      .withUsername("reshub")
      .withPassword("reshub");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void register(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    r.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired
  WebTestClient client;

  @Autowired
  JdbcTemplate jdbc;

  private Seed seed;
  private UUID reservationId;

  @BeforeEach
  void setup() {
    truncateAll();
    seed = seedBaseline();
    reservationId = insertReservation(seed);
  }

  @Test
  void managerCanReadReservationInOwnHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.id").isEqualTo(reservationId.toString())
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId.toString());
  }

  @Test
  void agencyCanReadOwnAgencyReservation() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId.toString());
  }

  @Test
  void receptionistCanReadReservationInOwnHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void managerCannotReadReservationFromOtherHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("X-User-Id", UUID.randomUUID().toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", UUID.randomUUID().toString())
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCannotReadOtherAgencyReservation() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("X-User-Id", UUID.randomUUID().toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", UUID.randomUUID().toString())
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void missingActorHeadersIsUnauthorized() {
    client.get().uri("/reservations/{id}", reservationId)
      .exchange()
      .expectStatus().isUnauthorized();
  }

  @Test
  void missingReservationIsNotFound() {
    client.get().uri("/reservations/{id}", UUID.randomUUID())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isNotFound();
  }

  private UUID insertReservation(Seed s) {
    UUID id = UUID.randomUUID();
    jdbc.update(
      "insert into reservation " +
        "(id, hotel_id, agency_id, created_by_user_id, external_ref, status, arrival_date, departure_date, guest_name, adults, children) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      id,
      s.hotelId,
      s.agencyId,
      s.receptionistUserId,
      "ext-" + id.toString().substring(0, 8),
      "NEW",
      java.sql.Date.valueOf(LocalDate.of(2026, 2, 20)),
      java.sql.Date.valueOf(LocalDate.of(2026, 2, 22)),
      "Test Guest",
      2,
      0
    );
    return id;
  }

  private Seed seedBaseline() {
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
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      managerUserId,
      "manager-" + managerUserId.toString().substring(0, 8) + "@example.com",
      "hash",
      "MANAGER",
      hotelId
    );

    UUID receptionistUserId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      receptionistUserId,
      "reception-" + receptionistUserId.toString().substring(0, 8) + "@example.com",
      "hash",
      "RECEPTIONIST",
      hotelId
    );

    UUID agencyUserId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, agency_id) values (?, ?, ?, ?, ?)",
      agencyUserId,
      "agency-" + agencyUserId.toString().substring(0, 8) + "@example.com",
      "hash",
      "AGENCY",
      agencyId
    );

    return new Seed(hotelId, agencyId, managerUserId, receptionistUserId, agencyUserId);
  }

  private void truncateAll() {
    jdbc.execute("truncate table reservation_comment, reservation, room_type_channel_map, room_type, app_user, agency, hotel restart identity cascade");
  }

  private record Seed(
    UUID hotelId,
    UUID agencyId,
    UUID managerUserId,
    UUID receptionistUserId,
    UUID agencyUserId
  ) {}
}
