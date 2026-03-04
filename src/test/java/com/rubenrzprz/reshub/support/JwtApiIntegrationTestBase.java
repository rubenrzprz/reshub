package com.rubenrzprz.reshub.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

public abstract class JwtApiIntegrationTestBase extends PostgresIntegrationTestBase {

  protected static final String VALID_PASSWORD = "secret123";
  protected static final String TEST_JWT_SECRET = "test-secret-key-with-at-least-32-characters";
  protected static final String TEST_JWT_ISSUER = "reshub-test";
  protected static final String TEST_JWT_EXPIRATION_MINUTES = "60";

  @DynamicPropertySource
  static void registerAuthProperties(DynamicPropertyRegistry r) {
    r.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    r.add("security.jwt.issuer", () -> TEST_JWT_ISSUER);
    r.add("security.jwt.expiration-minutes", () -> TEST_JWT_EXPIRATION_MINUTES);
  }

  @Autowired
  protected WebTestClient client;

  @Autowired
  protected ObjectMapper objectMapper;

  protected String issueToken(String email) {
    String body = client.post().uri("/auth/token")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of("email", email, "password", JwtApiIntegrationTestBase.VALID_PASSWORD))
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult()
      .getResponseBody();

    return readJson(body).path("accessToken").asText();
  }

  protected JsonNode readJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
