package com.rubenrzprz.reshub.reservation;

import java.util.List;

public record ReservationExportResponse(
  List<ReservationView> items
) {
}
