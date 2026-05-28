package com.rubenrzprz.reshub.flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayDevSeedIT {

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("reshub-dev-seed")
      .withUsername("reshub")
      .withPassword("reshub");

  static {
    postgres.start();
  }

  @AfterAll
  static void stopPostgres() {
    postgres.stop();
  }

  @Test
  void devSeedCreatesDemoLoginPath() throws SQLException {
    Flyway.configure()
      .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
      .locations("classpath:db/migration", "classpath:db/dev")
      .load()
      .migrate();

    try (Connection connection = DriverManager.getConnection(
      postgres.getJdbcUrl(),
      postgres.getUsername(),
      postgres.getPassword()
    )) {
      int userCount = count(
        connection,
        "select count(*) from app_user where email in (?, ?, ?, ?)",
        "admin@reshub.local",
        "manager@reshub.local",
        "reception@reshub.local",
        "agency@reshub.local"
      );
      int reservationCount = count(
        connection,
        "select count(*) from reservation where external_ref = ?",
        "DEMO-RES-001"
      );
      int authCount = count(
        connection,
        "select count(*) from agency_hotel_auth where status = 'ACTIVE'"
      );

      Assertions.assertEquals(4, userCount);
      Assertions.assertEquals(1, reservationCount);
      Assertions.assertTrue(authCount >= 1);
    }
  }

  private static int count(Connection connection, String sql, String... values)
    throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) {
        statement.setString(i + 1, values[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        Assertions.assertTrue(resultSet.next());
        return resultSet.getInt(1);
      }
    }
  }
}
