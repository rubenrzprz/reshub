package com.rubenrzprz.reshub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

class AuthApiIT extends AuthApiIntegrationTestBase {
  private static final String INVALID_PASSWORD = "wrong-password";

  @Test
  void validCredentialsReturnsToken() {
    String body = client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", seed.managerEmail(), "password", VALID_PASSWORD))
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
      .bodyValue(Map.of("email", seed.managerEmail(), "password", INVALID_PASSWORD))
      .exchange()
      .expectStatus().isUnauthorized()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_credentials");
  }

  @Test
  void invalidAuthPayloadReturnsValidationProblem() {
    client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", "not-an-email", "password", VALID_PASSWORD))
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("validation_failed")
      .jsonPath("$.errors[0].field").isEqualTo("email");
  }

  @Test
  void malformedAuthJsonReturnsInvalidRequestBody() {
    client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{")
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody()
      .jsonPath("$.code").isEqualTo("invalid_request_body");
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
    String token = issueToken(seed.managerEmail());
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "Bearer " + token)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo(reservationId.toString())
      .jsonPath("$.hotelId").isEqualTo(seed.hotelId().toString());
  }

  @Test
  void lowercaseBearerSchemeAllowsProtectedAccess() {
    String token = issueToken(seed.managerEmail());
    client.get().uri("/reservations/{id}", reservationId)
      .header("Authorization", "bearer " + token)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo(reservationId.toString());
  }

  @Test
  void validBearerStillEnforcesScope() {
    UUID otherReservation = dataFactory.insertReservation(
      seed,
      seed.otherHotelId(),
      seed.agencyId(),
      seed.otherManagerUserId(),
      "NEW",
      LocalDate.of(2026, 3, 4)
    );

    String token = issueToken(seed.managerEmail());

    client.get().uri("/reservations/{id}", otherReservation)
      .header("Authorization", "Bearer " + token)
      .exchange()
      .expectStatus().isForbidden()
      .expectBody()
      .jsonPath("$.code").isEqualTo("forbidden_scope");
  }
}
