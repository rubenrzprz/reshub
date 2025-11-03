package com.rubenrzprz.reshub.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthApiTest {

  @Value("${spring.application.name}")
  String appName;

  @Autowired
  WebTestClient client;

  @Test
  void health_returns_ok_with_configured_app_name() {
    client.get().uri("/health")
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.app").isEqualTo(appName)
      .jsonPath("$.status").isEqualTo("ok");
  }
}
