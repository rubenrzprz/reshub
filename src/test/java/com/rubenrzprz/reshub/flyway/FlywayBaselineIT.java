package com.rubenrzprz.reshub.flyway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
class FlywayBaselineIT extends FlywayIntegrationTestBase {

  @Test
  void v1TablesExist() {
    int hotel = tableExists("hotel");
    int agency = tableExists("agency");
    int appUser = tableExists("app_user");
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
}
