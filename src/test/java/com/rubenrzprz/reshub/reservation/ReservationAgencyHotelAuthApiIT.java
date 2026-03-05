package com.rubenrzprz.reshub.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

@TestPropertySource(properties = "features.enforce-agency-hotel-auth=true")
public class ReservationAgencyHotelAuthApiIT extends ReservationApiIntegrationTestBase {

  @Test
  void agencyCreateAllowedActiveAuthCoversArrivalDate() {
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 12, 31)
    );

    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + agencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-agency-auth-ok"))
      .exchange()
      .expectStatus().isCreated();
  }

  @Test
  void agencyCreateDeniedWhenNoAuthRow() {
    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + agencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-agency-auth-ok"))
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("agency_not_authorized_for_hotel");
  }

  @Test
  void agencyReadDeniedWhenAuthDoesNotCoverArrivalDate() {
    // baseline reservation arrival_date is 2026-02-20 in test factory
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 2, 21),
      LocalDate.of(2026, 12, 31)
    );

    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("agency_not_authorized_for_hotel");
  }

  @Test
  void agencyReadAllowedWhenAuthCoversArrivalDate() {
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 12, 31)
    );

    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void agencyListReturnsOnlyAuthorizedHotelsByArrivalDate() {
    //unauthorized hotel reservation
    dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.agencyId(),
      seed.agencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 20)
    );

    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 12, 31)
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].id").isEqualTo(reservationId.toString());
  }

  @Test
  void nonAgencyRoleUnchangedWhenFlagEnabled() {
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isOk();

    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + managerToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-manager-unchanged"))
      .exchange()
      .expectStatus().isCreated();
  }

  @Test
  void agencyCreateDeniedWhenAuthStatusIsSuspended() {
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "SUSPENDED",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 12, 31)
    );

    client.post().uri("/reservations")
      .header("Authorization", "Bearer " + agencyToken)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createPayload(seed.hotelId(), seed.agencyId(), "ext-agency-suspended"))
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("agency_not_authorized_for_hotel");
  }

  @Test
  void agencyReadAllowedOnValidityBoundaries() {
    // reservationId has arrival_date 2026-02-20 from test factory
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 2, 20),
      LocalDate.of(2026, 2, 20)
    );

    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void agencyReadAllowedWhenValidFromIsNullAndValidToCoversArrivalDate() {
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      null,
      LocalDate.of(2026, 2, 20)
    );

    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void agencyReadAllowedWhenValidToIsNullAndValidFromCoversArrivalDate() {
    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 2, 20),
      null
    );

    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void agencyListExcludesSameHotelReservationsOutsideAuthDateRange() {
    // same hotel out of range
    dataFactory.insertReservation(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.agencyUserId(),
      "NEW",
      LocalDate.of(2026, 2, 25)
    );

    dataFactory.insertAgencyHotelAuth(
      seed.agencyId(),
      seed.hotelId(),
      "ACTIVE",
      LocalDate.of(2026, 2, 19),
      LocalDate.of(2026, 2, 21)
    );

    client.get().uri(uriBuilder -> uriBuilder.path("/reservations").queryParam("limit", 50).build())
      .header("Authorization", "Bearer " + agencyToken)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].id").isEqualTo(reservationId.toString());
  }
}
