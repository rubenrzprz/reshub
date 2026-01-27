package com.rubenrzprz.reshub;

import org.junit.jupiter.api.Assertions;
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
abstract class FlywayITBase {

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

  protected int tableExists(String table) {
    Integer count = jdbc.queryForObject(
      "select count(*) from information_schema.tables " +
        "where table_schema='public' and table_name = ?",
      Integer.class,
      table
    );
    Assertions.assertNotNull(count);
    return count;
  }

  protected int constraintExists(String constraint) {
    Integer count = jdbc.queryForObject(
      "select count(*) from pg_constraint where conname = ?",
      Integer.class,
      constraint
    );
    Assertions.assertNotNull(count);
    return count;
  }

  protected int indexExists(String table, String index) {
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
