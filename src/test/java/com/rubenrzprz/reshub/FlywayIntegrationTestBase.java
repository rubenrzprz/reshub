package com.rubenrzprz.reshub;

import com.rubenrzprz.reshub.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
abstract class FlywayIntegrationTestBase extends PostgresIntegrationTestBase {

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
