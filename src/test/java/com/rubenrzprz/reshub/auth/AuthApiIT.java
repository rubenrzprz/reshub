package com.rubenrzprz.reshub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIT {
  private static final String VALID_PASSWORD = "secret123";
  private static final String INVALID_PASSWORD = "wrong-password";

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
    // Force JWT-only path for this test class
    r.add("security.jwt.allow-legacy-headers", () -> "false");
    r.add("security.jwt.secret", () -> "test-secret-key-with-at-least-32-characters");
    r.add("security.jwt.issuer", () -> "reshub-test");
    r.add("security.jwt.expiration-minutes", () -> "60");
  }

  @Autowired
  WebTestClient client;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  ObjectMapper objectMapper;

  private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  private Seed seed;
  private UUID reservationId;

  @BeforeEach
  void setup() {
    truncateAll();
    seed = seedBaseline();
    reservationId = insertReservation(seed, seed.hotelId, seed.agencyId, seed.managerUserId, "NEW", LocalDate.of(2026, 3, 3));
  }

  @Test
  void validCredentialsReturnsToken() {
    String body = client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", seed.managerEmail, "password", VALID_PASSWORD))
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult()
      .getResponseBody();

    JsonNode json = readJson(body);
    Assertions.assertNotNull(json.path("accessToken").asText());
    Assertions.assertFalse(json.path("accessToken").asText().isBlank());
    Assertions.assertEquals("Bearer", json.path("tokenType").asText());
    Assertions.assertTrue(json.path("expiresInSeconds").asLong() > 0);
  }

  @Test
  void invalidCredentialsReturnsUnauthorized() {
    client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", seed.managerEmail, "password", INVALID_PASSWORD))
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_credentials");
  }

  @Test
  void missingBearerOnReservationsReturnsUnauthorized() {
    client.get().uri("/reservations")
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("unauthorized_actor_context");
  }

  @Test
  void invalidBearerReturnsUnauthorized() {
    client.get().uri("/reservations")
      .header("Authorization", "Bearer not-a-valid-jwt")
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("unauthorized_actor_context");
  }

  @Test
  void validBearerAllowsProtectedAccess() {
    String token = issueToken(seed.managerEmail, VALID_PASSWORD);
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + token)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo(reservationId.toString())
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId.toString());
  }

  @Test
  void validBearerStillEnforcesScope() {
    UUID otherReservation = insertReservation(
      seed,
      seed.otherHotelId,
      seed.agencyId,
      seed.otherManagerUserId,
      "NEW",
      LocalDate.of(2026, 3, 4)
    );

    String token = issueToken(seed.managerEmail, VALID_PASSWORD);

    client.get().uri("/reservations/{id}", otherReservation)
      .header("Authorization", "Bearer " + token)
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("forbidden_scope");
  }

  private String issueToken(String email, String password) {
    String body = client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", email, "password", password))
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult()
      .getResponseBody();

    return readJson(body).path("accessToken").asText();
  }

  private JsonNode readJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private Seed seedBaseline() {
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
      encoder.encode(VALID_PASSWORD),
      "MANAGER",
      hotelId
    );

    UUID otherManagerUserId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      otherManagerUserId,
      "other-manager-" + otherManagerUserId.toString().substring(0, 8) + "@example.com",
      encoder.encode(VALID_PASSWORD),
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

  private UUID insertReservation(
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

  private void truncateAll() {
    jdbc.execute("truncate table reservation_comment, reservation, room_type_channel_map, room_type, app_user, agency, hotel restart identity cascade");
  }

  private record Seed(
    UUID hotelId,
    UUID otherHotelId,
    UUID agencyId,
    UUID managerUserId,
    String managerEmail,
    UUID otherManagerUserId
  ) {
  }
}
