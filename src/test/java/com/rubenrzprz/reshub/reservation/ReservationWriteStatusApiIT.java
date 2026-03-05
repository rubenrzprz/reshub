package com.rubenrzprz.reshub.reservation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class ReservationWriteStatusApiIT extends ReservationApiIntegrationTestBase {

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
}
