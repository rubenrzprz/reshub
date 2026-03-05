package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgencyHotelAuthorizationService {

  private final JdbcTemplate jdbc;
  private final ReservationFeatureFlagsProperties flags;

  public AgencyHotelAuthorizationService(JdbcTemplate jdbc, ReservationFeatureFlagsProperties flags) {
    this.jdbc = jdbc;
    this.flags = flags;
  }

  public boolean isAgencyAuthorizedForHotelOnDate(RequestActor actor, UUID hotelId, LocalDate date) {
    if (!isAgencyAuthEnforced(actor)) {
      return true;
    }

    Integer count = jdbc.queryForObject(
      "select count(*) " +
        "from agency_hotel_auth " +
        "where agency_id = ? " +
        "  and hotel_id = ? " +
        "  and status = 'ACTIVE' " +
        "  and (valid_from is null or valid_from <= ?) " +
        "  and (valid_to is null or valid_to >= ?)",
      Integer.class,
      actor.agencyId(),
      hotelId,
      date,
      date
    );

    return count != null && count > 0;
  }

  public boolean isAgencyAuthEnforced(RequestActor actor) {
    return flags.enforceAgencyHotelAuth() && actor.role() == RequestActor.Role.AGENCY;
  }
}
