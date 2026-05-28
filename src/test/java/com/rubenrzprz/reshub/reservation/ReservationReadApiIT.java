package com.rubenrzprz.reshub.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

public class ReservationReadApiIT extends ReservationApiIntegrationTestBase {

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
  void invalidDateQueryParameterIsBadRequest() {
    client.get().uri(uriBuilder -> uriBuilder.path("/reservations")
        .queryParam("arrivalFrom", "not-a-date")
        .queryParam("limit", 50)
        .build())
      .header("Authorization", "Bearer " + managerToken)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_request_parameter")
      .jsonPath("$.parameter").isEqualTo("arrivalFrom");
  }
}
