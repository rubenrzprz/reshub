package com.rubenrzprz.reshub.flyway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
class FlywayRoomModelingIT extends FlywayIntegrationTestBase {

  @Test
  void v2TablesExist() {
    Assertions.assertEquals(1, tableExists("room_type"));
    Assertions.assertEquals(1, tableExists("room_type_channel_map"));
  }

  @Test
  void v2ConstraintsAndIndexesExist() {
    Assertions.assertEquals(1, constraintExists("room_type_code_per_hotel"));
    Assertions.assertEquals(1, constraintExists("room_type_channel_map_unique"));
    Assertions.assertEquals(1, indexExists("room_type", "idx_room_type_attributes_canonical"));
  }
}
