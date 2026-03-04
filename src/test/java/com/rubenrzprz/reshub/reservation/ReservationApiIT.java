package com.rubenrzprz.reshub.reservation;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationApiIT extends ReservationApiIntegrationTestBase {

  @Test
  void managerCanReadReservationInOwnHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.id").isEqualTo(reservationId.toString())
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void agencyCanReadOwnAgencyReservation() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId().toString());
  }

  @Test
  void receptionistCanReadReservationInOwnHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + receptionistToken)
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void adminCanReadReservationAcrossHotels() {
    UUID otherReservationId = dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21)
    );

    client.get().uri("/reservations/{id}", otherReservationId)
      .header("Authorization", "Bearer " + adminToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo(otherReservationId.toString())
      .jsonPath("$.hotelId").isEqualTo(seed.otherHotelId().toString())
      .jsonPath("$.agencyId").isEqualTo(seed.otherAgencyId().toString());
  }

  @Test
  void managerCannotReadReservationFromOtherHotel() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + otherManagerToken)
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("forbidden_scope");
  }

  @Test
  void agencyCannotReadOtherAgencyReservation() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + otherAgencyToken)
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void missingActorHeadersIsUnauthorized() {
    client.get().uri("/reservations/{id}", reservationId)
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("unauthorized_actor_context");
  }

  @Test
  void missingReservationIsNotFound() {
    client.get().uri("/reservations/{id}", UUID.randomUUID())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isNotFound()
      .expectBody()
      .jsonPath("$.code").isEqualTo("reservation_not_found");
  }

  @Test
  void managerCanUpdateNotesInOwnHotel() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "manager update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("manager update");
  }

  @Test
  void adminCanUpdateNotesAcrossHotels() {
    UUID otherReservationId = dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21)
    );

    client.patch().uri("/reservations/{id}/notes", otherReservationId)
      .header("Authorization", "Bearer " + adminToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "admin cross-tenant update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("admin cross-tenant update");
  }

  @Test
  void receptionistCanUpdateOwnReservationNotes() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("Authorization", "Bearer " + receptionistToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "reception update"))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.notes").isEqualTo("reception update");
  }

  @Test
  void receptionistCannotUpdateReservationCreatedByAnotherUser() {
    UUID managerCreatedReservation = dataFactory.insertReservation(seed, seed.managerUserId());

    client.patch().uri("/reservations/{id}/notes", managerCreatedReservation)
      .header("Authorization", "Bearer " + receptionistToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void managerCannotUpdateReservationFromOtherHotel() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("Authorization", "Bearer " + otherManagerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCanUpdateOwnAgencyReservationNotes() {
    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
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
      .header("Authorization", "Bearer " + otherAgencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "forbidden update"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void managerCanCreateInternalCommentInOwnHotel() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "manager internal note"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.reservationId").isEqualTo(reservationId.toString())
      .jsonPath("$.authorUserId").isEqualTo(seed.managerUserId().toString())
      .jsonPath("$.body").isEqualTo("manager internal note");

    assertCommentCount(reservationId, 1);
  }

  @Test
  void adminCanCreateCommentAcrossHotels() {
    UUID otherReservationId = dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21)
    );

    client.post().uri("/reservations/{id}/comments", otherReservationId)
      .header("Authorization", "Bearer " + adminToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "admin cross-tenant comment"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.reservationId").isEqualTo(otherReservationId.toString())
      .jsonPath("$.authorUserId").isEqualTo(seed.adminUserId().toString())
      .jsonPath("$.body").isEqualTo("admin cross-tenant comment");

    assertCommentCount(otherReservationId, 1);
  }

  @Test
  void receptionistCanCreateInternalCommentInOwnHotel() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("Authorization", "Bearer " + receptionistToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "reception internal note"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.authorUserId").isEqualTo(seed.receptionistUserId().toString());

    assertCommentCount(reservationId, 1);
  }

  @Test
  void managerCannotCreateCommentForOtherHotelReservation() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("Authorization", "Bearer " + otherManagerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "forbidden note"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCannotCreateInternalCommentEvenOnOwnReservation() {
    client.post().uri("/reservations/{id}/comments", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
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
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("body", "missing reservation"))
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  void managerCanConfirmReservationInOwnHotel() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("CONFIRMED");

    Assertions.assertEquals("CONFIRMED", loadStatus(reservationId));
  }

  @Test
  void adminCanChangeStatusAcrossHotels() {
    UUID otherReservationId = dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21)
    );

    client.post().uri("/reservations/{id}/confirm", otherReservationId)
      .header("Authorization", "Bearer " + adminToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("CONFIRMED");

    Assertions.assertEquals("CONFIRMED", loadStatus(otherReservationId));
  }

  @Test
  void receptionistCanCancelOwnReservation() {
    client.post().uri("/reservations/{id}/cancel", reservationId)
      .header("Authorization", "Bearer " + receptionistToken)
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
    UUID managerCreatedReservation = dataFactory.insertReservation(seed, seed.managerUserId());

    client.post().uri("/reservations/{id}/cancel", managerCreatedReservation)
      .header("Authorization", "Bearer " + receptionistToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("reason", "forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void agencyCanMarkOwnReservationAsNoShow() {
    client.post().uri("/reservations/{id}/noshow", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("NOSHOW");

    Assertions.assertEquals("NOSHOW", loadStatus(reservationId));
  }

  @Test
  void agencyCannotChangeStatusForOtherAgencyReservation() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("Authorization", "Bearer " + otherAgencyToken)
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void invalidStatusTransitionReturnsConflict() {
    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk();

    client.post().uri("/reservations/{id}/confirm", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_status_transition");
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
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-create-manager"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId().toString())
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId().toString())
      .jsonPath("$.createdByUserId").isEqualTo(seed.managerUserId().toString())
      .jsonPath("$.status").isEqualTo("NEW");
  }

  @Test
  void adminCanCreateReservationAcrossHotels() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + adminToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.otherHotelId(), seed.otherAgencyId(), "ext-create-admin"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.hotelId").isEqualTo(seed.otherHotelId().toString())
      .jsonPath("$.agencyId").isEqualTo(seed.otherAgencyId().toString())
      .jsonPath("$.createdByUserId").isEqualTo(seed.adminUserId().toString())
      .jsonPath("$.status").isEqualTo("NEW");
  }

  @Test
  void managerCannotCreateReservationForOtherHotel() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.otherHotelId(), seed.agencyId(), "ext-create-manager-forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void receptionistCanCreateReservationInOwnHotel() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + receptionistToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-create-reception"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.createdByUserId").isEqualTo(seed.receptionistUserId().toString());
  }

  @Test
  void agencyCanCreateReservationForOwnAgency() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + agencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-create-agency"))
      .exchange()
      .expectStatus().isCreated()
      .expectBody()
      .jsonPath("$.agencyId").isEqualTo(seed.agencyId().toString())
      .jsonPath("$.createdByUserId").isEqualTo(seed.agencyUserId().toString());
  }

  @Test
  void agencyCannotCreateReservationForOtherAgency() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + agencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.otherAgencyId(), "ext-create-agency-forbidden"))
      .exchange()
      .expectStatus().isForbidden();
  }

  @Test
  void duplicateExternalReferenceReturnsConflict() {
    Map<String, Object> payload = createPayload(seed.hotelId(), seed.agencyId(), "ext-create-dup");

    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .exchange()
      .expectStatus().isCreated();

    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody()
      .jsonPath("$.code").isEqualTo("duplicate_external_ref");
  }

  @Test
  void invalidCreatePayloadReturnsBadRequest() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of(
        "hotelId", seed.hotelId().toString(),
        "agencyId", seed.agencyId().toString(),
        "externalRef", "ext-create-invalid",
        "arrivalDate", "2026-02-25",
        "departureDate", "2026-02-25",
        "guestName", "Bad Payload Guest",
        "adults", 1,
        "children", 0
      ))
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_reservation_payload");
  }

  @Test
  void cancelledReservationCannotBeUpdated() {
    client.post().uri("/reservations/{id}/cancel", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("reason", "cancelled before arrival"))
      .exchange()
      .expectStatus().isOk();

    client.patch().uri("/reservations/{id}/notes", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("notes", "attempting post-cancel update"))
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_status_transition");
  }

  @Test
  void createMissingActorHeadersIsUnauthorized() {
    client.post().uri("/reservations")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-create-no-actor"))
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("unauthorized_actor_context");
  }

  @Test
  void managerListIsScopedToOwnHotel() {
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "NEW", LocalDate.of(2026, 2, 21));
    dataFactory.insertReservation(seed, seed.otherHotelId(), seed.otherAgencyId(), seed.otherManagerUserId(), "NEW", LocalDate.of(2026, 2, 22));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(2)
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId().toString())
      .jsonPath("$.items[1].hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void adminListCanIncludeMultipleHotels() {
    dataFactory.insertReservation(seed, seed.otherHotelId(), seed.otherAgencyId(), seed.otherManagerUserId(), "NEW", LocalDate.of(2026, 2, 21));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("Authorization", "Bearer " + adminToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(2)
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId().toString())
      .jsonPath("$.items[1].hotelId").isEqualTo(seed.otherHotelId().toString());
  }

  @Test
  void agencyListIsScopedToOwnAgency() {
    dataFactory.insertReservation(seed, seed.hotelId(), seed.otherAgencyId(), seed.otherAgencyUserId(), "NEW", LocalDate.of(2026, 2, 21));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].agencyId").isEqualTo(seed.agencyId().toString());
  }

  @Test
  void keysetPaginationReturnsNextCursorAndSecondPage() throws Exception {
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "NEW", LocalDate.of(2026, 2, 21));
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "NEW", LocalDate.of(2026, 2, 22));

    String page1Body = client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 2).build())
      .header("Authorization", "Bearer " + managerToken)
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
      .header("Authorization", "Bearer " + managerToken)
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
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_cursor");
  }

  @Test
  void limitAboveMaxIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 201).build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_limit");
  }

  @Test
  void statusFilterAppliesWithinScope() {
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "CONFIRMED", LocalDate.of(2026, 2, 23));
    dataFactory.insertReservation(seed, seed.otherHotelId(), seed.otherAgencyId(), seed.otherManagerUserId(), "CONFIRMED", LocalDate.of(2026, 2, 24));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("limit", 50)
        .queryParam("status", "CONFIRMED")
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].status").isEqualTo("CONFIRMED")
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void arrivalDateRangeFilterAppliesWithinManagerScope() {
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "NEW", LocalDate.of(2026, 2, 18));
    dataFactory.insertReservation(seed, seed.hotelId(), seed.agencyId(), seed.managerUserId(), "NEW", LocalDate.of(2026, 2, 23));
    dataFactory.insertReservation(seed, seed.otherHotelId(), seed.otherAgencyId(), seed.otherManagerUserId(), "NEW", LocalDate.of(2026, 2, 22));

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("arrivalFrom", "2026-02-20")
        .queryParam("arrivalTo", "2026-02-22")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].arrivalDate").isEqualTo("2026-02-20")
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void guestQueryFiltersByNameWithinManagerScope() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Alice Smith",
      "alice@example.com",
      "+34111111111"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 22),
      "Bob Johnson",
      "bob@example.com",
      "+34222222222"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 22),
      "Alice Outside",
      "outside@example.com",
      "+34333333333"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("guestQuery", "smith")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].guestName").isEqualTo("Alice Smith")
      .jsonPath("$.items[0].hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void guestQueryCanMatchEmailWithinAgencyScope() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.agencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Agency Match",
      "match@agency.com",
      "+34111111111"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.otherAgencyId(),
      seed.otherAgencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 22),
      "Other Agency Match",
      "match@agency.com",
      "+34222222222"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("guestQuery", "MATCH@AGENCY.COM")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].agencyId").isEqualTo(seed.agencyId().toString())
      .jsonPath("$.items[0].guestName").isEqualTo("Agency Match");
  }

  @Test
  void combinedStatusDateAndGuestFiltersReturnIntersection() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "CONFIRMED",
      LocalDate.of(2026, 2, 21),
      "Alice Combined",
      "alice-combined@example.com",
      "+34111111111"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Alice Wrong Status",
      "alice-wrong@example.com",
      "+34222222222"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "CONFIRMED",
      LocalDate.of(2026, 2, 25),
      "Alice Wrong Date",
      "alice-date@example.com",
      "+34333333333"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("status", "CONFIRMED")
        .queryParam("arrivalFrom", "2026-02-20")
        .queryParam("arrivalTo", "2026-02-22")
        .queryParam("guestQuery", "alice")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].status").isEqualTo("CONFIRMED")
      .jsonPath("$.items[0].arrivalDate").isEqualTo("2026-02-21")
      .jsonPath("$.items[0].guestName").isEqualTo("Alice Combined");
  }

  @Test
  void invalidDateRangeIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("arrivalFrom", "2026-02-23")
        .queryParam("arrivalTo", "2026-02-22")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_date_range");
  }

  @Test
  void managerCanExportJsonWithFilters() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "CONFIRMED",
      LocalDate.of(2026, 2, 21),
      "Export Match",
      "export-match@example.com",
      "+34111111111"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Export Wrong Status",
      "export-wrong@example.com",
      "+34222222222"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations/export")
        .queryParam("status", "CONFIRMED")
        .queryParam("arrivalFrom", "2026-02-20")
        .queryParam("arrivalTo", "2026-02-22")
        .queryParam("guestQuery", "match")
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].status").isEqualTo("CONFIRMED")
      .jsonPath("$.items[0].guestName").isEqualTo("Export Match");
  }

  @Test
  void agencyExportJsonRemainsScopedToOwnAgency() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.agencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Agency Export Match",
      "agency-export@example.com",
      "+34111111111"
    );
    dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.otherAgencyId(),
      seed.otherAgencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "Other Agency Export Match",
      "agency-export@example.com",
      "+34222222222"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations/export")
        .queryParam("guestQuery", "agency-export@example.com")
        .build())
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].agencyId").isEqualTo(seed.agencyId().toString())
      .jsonPath("$.items[0].guestName").isEqualTo("Agency Export Match");
  }

  @Test
  void adminCanExportCsvAcrossHotelsWithHeaders() {
    dataFactory.insertReservationWithGuest(
      seed,
      seed.otherHotelId(),
      seed.otherAgencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "CSV Admin Match",
      "csv-admin@example.com",
      "+34111111111"
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations/export.csv")
        .queryParam("guestQuery", "csv-admin@example.com")
        .build())
      .header("Authorization", "Bearer " + adminToken)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv"))
      .expectHeader().valueMatches("Content-Disposition", ".*reservations-export\\.csv.*")
      .expectBody(String.class)
      .value(body -> {
        Assertions.assertTrue(body.startsWith("id,hotelId,agencyId,createdByUserId,status,arrivalDate,departureDate,guestName,notes"));
        Assertions.assertTrue(body.contains("\"CSV Admin Match\""));
        Assertions.assertTrue(body.contains(seed.otherHotelId().toString()));
      });
  }

  @Test
  void exportInvalidDateRangeIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations/export")
        .queryParam("arrivalFrom", "2026-02-23")
        .queryParam("arrivalTo", "2026-02-22")
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_date_range");
  }

  @Test
  void csvExportNeutralizesFormulaLikeCells() {
    UUID formulaReservationId = dataFactory.insertReservationWithGuest(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 2, 21),
      "=2+2",
      "formula@example.com",
      "+34111111111"
    );
    jdbc.update("update reservation set notes = ? where id = ?", "@cmd", formulaReservationId);

    client.get().uri("/reservations/export.csv")
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .value(body -> {
        Assertions.assertTrue(body.contains("\"'=2+2\""));
        Assertions.assertTrue(body.contains("\"'@cmd\""));
      });
  }
}


