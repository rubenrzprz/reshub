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

  @Test
  void v1ConstraintsAndIndexesExist() {
    Assertions.assertEquals(1, constraintExists("app_user_affiliation_xor"));
    Assertions.assertEquals(1, indexExists("app_user", "idx_app_user_hotel"));
    Assertions.assertEquals(1, indexExists("app_user", "idx_app_user_agency"));
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

  private int constraintExists(String constraint) {
    Integer count = jdbc.queryForObject(
      "select count(*) from pg_constraint where conname = ?",
      Integer.class,
      constraint
    );
    Assertions.assertNotNull(count);
    return count;
  }

  private int indexExists(String table, String index) {
    Integer count = jdbc.queryForObject(
      "select count(*) from pg_indexes where schemaname='public' " +
        "and tablename = ? and indexname = ?",
      Integer.class,
      table,
      index
    );
    Assertions.assertNotNull(count);
    return count;
  }
}
