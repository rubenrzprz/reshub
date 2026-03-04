package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.reservation.support.ReservationTestDataFactory;
import com.rubenrzprz.reshub.support.JwtApiIntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationApiIntegrationTestBase extends JwtApiIntegrationTestBase {

  @Autowired
  protected JdbcTemplate jdbc;

  protected ReservationTestDataFactory dataFactory;
  protected ReservationTestDataFactory.Seed seed;
  protected UUID reservationId;

  protected String adminToken;
  protected String managerToken;
  protected String receptionistToken;
  protected String agencyToken;
  protected String otherManagerToken;
  protected String otherAgencyToken;

  @BeforeEach
  void baseSetup() {
    dataFactory = new ReservationTestDataFactory(jdbc);
    dataFactory.truncateAll();
    seed = dataFactory.seedBaseline(VALID_PASSWORD);
    reservationId = dataFactory.insertReservation(seed);
    adminToken = issueToken(seed.adminEmail());
    managerToken = issueToken(seed.managerEmail());
    receptionistToken = issueToken(seed.receptionistEmail());
    agencyToken = issueToken(seed.agencyEmail());
    otherManagerToken = issueToken(seed.otherManagerEmail());
    otherAgencyToken = issueToken(seed.otherAgencyEmail());
  }

  protected Map<String, Object> createPayload(UUID hotelId, UUID agencyId, String externalRef) {
    return Map.of(
      "hotelId", hotelId.toString(),
      "agencyId", agencyId.toString(),
      "externalRef", externalRef,
      "arrivalDate", "2026-02-25",
      "departureDate", "2026-02-27",
      "guestName", "Create Payload Guest",
      "adults", 2,
      "children", 0,
      "notes", "Created via API test"
    );
  }


  protected void assertCommentCount(UUID reservationId, int expected) {
    Integer count = jdbc.queryForObject(
      "select count(*) from reservation_comment where reservation_id = ?",
      Integer.class,
      reservationId
    );
    Assertions.assertEquals(expected, count);
  }

  protected String loadStatus(UUID reservationId) {
    return jdbc.queryForObject(
      "select status from reservation where id = ?",
      String.class,
      reservationId
    );
  }

  protected Object loadCancelledAt(UUID reservationId) {
    return jdbc.queryForObject(
      "select cancelled_at from reservation where id = ?",
      Object.class,
      reservationId
    );
  }
}
