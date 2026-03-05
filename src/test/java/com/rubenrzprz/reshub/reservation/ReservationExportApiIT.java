package com.rubenrzprz.reshub.reservation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

public class ReservationExportApiIT extends ReservationApiIntegrationTestBase {

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
