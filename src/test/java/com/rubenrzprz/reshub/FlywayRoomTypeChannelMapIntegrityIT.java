package com.rubenrzprz.reshub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FlywayRoomTypeChannelMapIntegrityIT extends FlywayIntegrationTestBase {

  @Test
  void v3ConstraintsExist() {
    Assertions.assertEquals(1, constraintExists("room_type_hotel_id_id_unique"));
    Assertions.assertEquals(1, constraintExists("room_type_channel_map_hotel_fk"));
    Assertions.assertEquals(1, constraintExists("room_type_channel_map_room_type_hotel_fk"));
  }
}
