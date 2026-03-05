package com.rubenrzprz.reshub.auth;

import com.rubenrzprz.reshub.auth.support.AuthTestDataFactory;
import com.rubenrzprz.reshub.support.JwtApiIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthApiIntegrationTestBase extends JwtApiIntegrationTestBase {

  @Autowired
  protected JdbcTemplate jdbc;

  protected AuthTestDataFactory dataFactory;
  protected AuthTestDataFactory.Seed seed;
  protected UUID reservationId;

  @BeforeEach
  void baseSetup() {
    dataFactory = new AuthTestDataFactory(jdbc);
    dataFactory.truncateAll();
    seed = dataFactory.seedBaseline(VALID_PASSWORD);
    reservationId = dataFactory.insertReservation(
      seed,
      seed.hotelId(),
      seed.agencyId(),
      seed.managerUserId(),
      "NEW",
      LocalDate.of(2026, 3, 3)
    );
  }
}
