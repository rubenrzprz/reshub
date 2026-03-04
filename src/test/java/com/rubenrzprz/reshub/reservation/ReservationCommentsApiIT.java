package com.rubenrzprz.reshub.reservation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class ReservationCommentsApiIT extends ReservationApiIntegrationTestBase {

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
}
