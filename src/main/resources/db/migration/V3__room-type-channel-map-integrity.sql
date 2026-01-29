-- Enforce hotel integrity for room_type_channel_map
alter table room_type
  add constraint room_type_hotel_id_id_unique unique (hotel_id, id);

alter table room_type_channel_map
  add constraint room_type_channel_map_hotel_fk
    foreign key (hotel_id) references hotel(id) on delete restrict;

alter table room_type_channel_map
  drop constraint if exists room_type_channel_map_room_type_id_fkey;

alter table room_type_channel_map
  add constraint room_type_channel_map_room_type_hotel_fk
    foreign key (hotel_id, room_type_id)
      references room_type(hotel_id, id) on delete restrict;
