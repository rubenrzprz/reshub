package com.rubenrzprz.reshub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class FlywayBaselineIT {

  @SuppressWarnings("resource")
  @Container
  static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("reshub")
      .withUsername("reshub")
      .withPassword("reshub");

  @DynamicPropertySource
  static void register(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    r.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired
  JdbcTemplate jdbc;

  @Test
  void v1TablesExist() {
    int hotel = exists("hotel");
    int agency = exists("agency");
    int appUser = exists("app_user");
    Assertions.assertEquals(1, hotel);
    Assertions.assertEquals(1, agency);
    Assertions.assertEquals(1, appUser);
  }

  private int exists(String table) {
    Integer count = jdbc.queryForObject(
      "select count(*) from information_schema.tables " +
        "where table_schema='public' and table_name = ?",
      Integer.class,
      table
    );
    Assertions.assertNotNull(count);
    return count;
  }
}
