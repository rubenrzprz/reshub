package com.rubenrzprz.reshub.reservation;

import java.util.List;

public record ReservationListResponse(
  List<ReservationView> items,
  String nextCursor,
  int limit
) {
}
