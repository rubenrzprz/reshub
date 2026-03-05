package com.rubenrzprz.reshub.reservation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "features")
public record ReservationFeatureFlagsProperties(
  boolean enforceAgencyHotelAuth
) {
}
