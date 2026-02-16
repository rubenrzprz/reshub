package com.rubenrzprz.reshub.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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

  @Autowired
  ObjectMapper objectMapper;

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

  @Test
  void managerCanUpdateNotesInOwnHotel() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "manager update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("manager update");
  }

  @Test
  void receptionistCanUpdateOwnReservationNotes() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "reception update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("reception update");
  }

  @Test
  void receptionistCannotUpdateReservationCreatedByAnotherUser() {
    UUID managerCreatedReservation = insertReservation(seed, seed.managerUserId);

    client.patch().uri("/reservations/{id}/notes", managerCreatedReservation)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void managerCannotUpdateReservationFromOtherHotel() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCanUpdateOwnAgencyReservationNotes() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "agency update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("agency update");
  }

  @Test
  void agencyCannotUpdateOtherAgencyReservation() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void managerCanCreateInternalCommentInOwnHotel() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "manager internal note"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.reservationId").isEqualTo(reservationId.toString())
      .jsonPath("$.authorUserId").isEqualTo(seed.managerUserId.toString())
      .jsonPath("$.body").isEqualTo("manager internal note");

    assertCommentCount(reservationId, 1);
  }

  @Test
  void receptionistCanCreateInternalCommentInOwnHotel() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "reception internal note"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.authorUserId").isEqualTo(seed.receptionistUserId.toString());

    assertCommentCount(reservationId, 1);
  }

  @Test
  void managerCannotCreateCommentForOtherHotelReservation() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "forbidden note"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCannotCreateInternalCommentEvenOnOwnReservation() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "agency note"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void commentCreationMissingActorHeadersIsUnauthorized() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "no actor"))
      .exchange()
      .expectStatus().isUnauthorized();
  }

  @Test
  void commentCreationForMissingReservationIsNotFound() {
    client.post().uri("/reservations/{id}/comments", UUID.randomUUID())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "missing reservation"))
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  void managerCanConfirmReservationInOwnHotel() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("CONFIRMED");

    Assertions.assertEquals("CONFIRMED", loadStatus(reservationId));
  }

  @Test
  void receptionistCanCancelOwnReservation() {
    client.post().uri("/reservations/{id}/cancel", reservationId)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("reason", "guest requested"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("CANCELLED");

    Assertions.assertEquals("CANCELLED", loadStatus(reservationId));
    Assertions.assertNotNull(loadCancelledAt(reservationId));
  }

  @Test
  void receptionistCannotChangeStatusForReservationCreatedByAnotherUser() {
    UUID managerCreatedReservation = insertReservation(seed, seed.managerUserId);

    client.post().uri("/reservations/{id}/cancel", managerCreatedReservation)
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("reason", "forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCanMarkOwnReservationAsNoShow() {
    client.post().uri("/reservations/{id}/noshow", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("NOSHOW");

    Assertions.assertEquals("NOSHOW", loadStatus(reservationId));
  }

  @Test
  void agencyCannotChangeStatusForOtherAgencyReservation() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", UUID.randomUUID().toString())
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void invalidStatusTransitionReturnsConflict() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk();

    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isEqualTo(409);
  }

  @Test
  void statusActionMissingActorHeadersIsUnauthorized() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .exchange()
      .expectStatus().isUnauthorized();
  }

  @Test
  void managerCanCreateReservationInOwnHotel() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId, seed.agencyId, "ext-create-manager"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId.toString())
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId.toString())
      .jsonPath("$.createdByUserId").isEqualTo(seed.managerUserId.toString())
      .jsonPath("$.status").isEqualTo("NEW");
  }

  @Test
  void managerCannotCreateReservationForOtherHotel() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.otherHotelId, seed.agencyId, "ext-create-manager-forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void receptionistCanCreateReservationInOwnHotel() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.receptionistUserId.toString())
      .header("X-Role", "RECEPTIONIST")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId, seed.agencyId, "ext-create-reception"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.createdByUserId").isEqualTo(seed.receptionistUserId.toString());
  }

  @Test
  void agencyCanCreateReservationForOwnAgency() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId, seed.agencyId, "ext-create-agency"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId.toString())
      .jsonPath("$.createdByUserId").isEqualTo(seed.agencyUserId.toString());
  }

  @Test
  void agencyCannotCreateReservationForOtherAgency() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId, seed.otherAgencyId, "ext-create-agency-forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void duplicateExternalReferenceReturnsConflict() {
    Map<String, Object> payload = createPayload(seed.hotelId, seed.agencyId, "ext-create-dup");

    client.post().uri("/reservations")
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .exchange()
      .expectStatus().isCreated();

    client.post().uri("/reservations")
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .exchange()
      .expectStatus().isEqualTo(409);
  }

  @Test
  void invalidCreatePayloadReturnsBadRequest() {
    client.post().uri("/reservations")
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of(
        "hotelId", seed.hotelId.toString(),
        "agencyId", seed.agencyId.toString(),
        "externalRef", "ext-create-invalid",
        "arrivalDate", "2026-02-25",
        "departureDate", "2026-02-25",
        "guestName", "Bad Payload Guest",
        "adults", 1,
        "children", 0
      ))
      .exchange()
      .expectStatus().isBadRequest();
  }

  @Test
  void createMissingActorHeadersIsUnauthorized() {
    client.post().uri("/reservations")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId, seed.agencyId, "ext-create-no-actor"))
      .exchange()
      .expectStatus().isUnauthorized();
  }

  @Test
  void managerListIsScopedToOwnHotel() {
    insertReservation(seed, seed.hotelId, seed.agencyId, seed.managerUserId, "NEW", LocalDate.of(2026, 2, 21));
    insertReservation(seed, seed.otherHotelId, seed.otherAgencyId, seed.otherManagerUserId, "NEW", LocalDate.of(2026, 2, 22));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(2)
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId.toString())
      .jsonPath("$.items[1].hotelId").isEqualTo(seed.hotelId.toString());
  }

  @Test
  void agencyListIsScopedToOwnAgency() {
    insertReservation(seed, seed.hotelId, seed.otherAgencyId, seed.otherAgencyUserId, "NEW", LocalDate.of(2026, 2, 21));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("X-User-Id", seed.agencyUserId.toString())
      .header("X-Role", "AGENCY")
      .header("X-Agency-Id", seed.agencyId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].agencyId").isEqualTo(seed.agencyId.toString());
  }

  @Test
  void keysetPaginationReturnsNextCursorAndSecondPage() throws Exception {
    insertReservation(seed, seed.hotelId, seed.agencyId, seed.managerUserId, "NEW", LocalDate.of(2026, 2, 21));
    insertReservation(seed, seed.hotelId, seed.agencyId, seed.managerUserId, "NEW", LocalDate.of(2026, 2, 22));

    String page1Body = client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 2).build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult()
      .getResponseBody();

    JsonNode firstPage = objectMapper.readTree(page1Body);
    Assertions.assertEquals(2, firstPage.path("items").size());
    Assertions.assertEquals("2026-02-20", firstPage.path("items").get(0).path("arrivalDate").asText());
    Assertions.assertEquals("2026-02-21", firstPage.path("items").get(1).path("arrivalDate").asText());
    String nextCursor = firstPage.path("nextCursor").asText();
    Assertions.assertFalse(nextCursor == null || nextCursor.isBlank());

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 2).queryParam("cursor", nextCursor).build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].arrivalDate").isEqualTo("2026-02-22")
      .jsonPath("$.nextCursor").isEmpty();
  }

  @Test
  void invalidCursorIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("cursor", "invalid").build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isBadRequest();
  }

  @Test
  void limitAboveMaxIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 201).build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isBadRequest();
  }

  @Test
  void statusFilterAppliesWithinScope() {
    insertReservation(seed, seed.hotelId, seed.agencyId, seed.managerUserId, "CONFIRMED", LocalDate.of(2026, 2, 23));
    insertReservation(seed, seed.otherHotelId, seed.otherAgencyId, seed.otherManagerUserId, "CONFIRMED", LocalDate.of(2026, 2, 24));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
      .queryParam("limit", 50)
      .queryParam("status", "CONFIRMED")
      .build())
      .header("X-User-Id", seed.managerUserId.toString())
      .header("X-Role", "MANAGER")
      .header("X-Hotel-Id", seed.hotelId.toString())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].status").isEqualTo("CONFIRMED")
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId.toString());
  }

  private UUID insertReservation(Seed s) {
    return insertReservation(s, s.receptionistUserId);
  }

  private Map<String, Object> createPayload(UUID hotelId, UUID agencyId, String externalRef) {
    return Map.of(
      "hotelId", hotelId.toString(),
      "agencyId", agencyId.toString(),
      "externalRef", externalRef,
      "arrivalDate", "2026-02-25",
      "departureDate", "2026-02-27",
      "guestName", "Create Payload Guest",
      "adults", 2,
      "children", 0,
      "notes", "Created via API test"
    );
  }

  private UUID insertReservation(Seed s, UUID createdByUserId) {
    return insertReservation(s, s.hotelId, s.agencyId, createdByUserId, "NEW", LocalDate.of(2026, 2, 20));
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
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, hotel_id) values (?, ?, ?, ?, ?)",
      otherManagerUserId,
      "other-manager-" + otherManagerUserId.toString().substring(0, 8) + "@example.com",
      "hash",
      "MANAGER",
      otherHotelId
    );

    UUID otherAgencyUserId = UUID.randomUUID();
    jdbc.update(
      "insert into app_user (id, email, password_hash, role, agency_id) values (?, ?, ?, ?, ?)",
      otherAgencyUserId,
      "other-agency-" + otherAgencyUserId.toString().substring(0, 8) + "@example.com",
      "hash",
      "AGENCY",
      otherAgencyId
    );

    return new Seed(
      hotelId,
      agencyId,
      managerUserId,
      receptionistUserId,
      agencyUserId,
      otherHotelId,
      otherAgencyId,
      otherManagerUserId,
      otherAgencyUserId
    );
  }

  private void truncateAll() {
    jdbc.execute("truncate table reservation_comment, reservation, room_type_channel_map, room_type, app_user, agency, hotel restart identity cascade");
  }

  private void assertCommentCount(UUID reservationId, int expected) {
    Integer count = jdbc.queryForObject(
      "select count(*) from reservation_comment where reservation_id = ?",
      Integer.class,
      reservationId
    );
    Assertions.assertEquals(expected, count);
  }

  private String loadStatus(UUID reservationId) {
    return jdbc.queryForObject(
      "select status from reservation where id = ?",
      String.class,
      reservationId
    );
  }

  private Object loadCancelledAt(UUID reservationId) {
    return jdbc.queryForObject(
      "select cancelled_at from reservation where id = ?",
      Object.class,
      reservationId
    );
  }

  private record Seed(
    UUID hotelId,
    UUID agencyId,
    UUID managerUserId,
    UUID receptionistUserId,
    UUID agencyUserId,
    UUID otherHotelId,
    UUID otherAgencyId,
    UUID otherManagerUserId,
    UUID otherAgencyUserId
  ) {}
}
