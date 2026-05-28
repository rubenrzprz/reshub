package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.persistence.AgencyHotelAuthRepository;
import com.rubenrzprz.reshub.security.RequestActor;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgencyHotelAuthorizationService {

  private final AgencyHotelAuthRepository agencyHotelAuthRepository;
  private final ReservationFeatureFlagsProperties flags;

  public AgencyHotelAuthorizationService(
    AgencyHotelAuthRepository agencyHotelAuthRepository,
    ReservationFeatureFlagsProperties flags
  ) {
    this.agencyHotelAuthRepository = agencyHotelAuthRepository;
    this.flags = flags;
  }

  public boolean isAgencyAuthorizedForHotelOnDate(RequestActor actor, UUID hotelId, LocalDate date) {
    if (!isAgencyAuthEnforced(actor)) {
      return true;
    }

    return agencyHotelAuthRepository.existsActiveCoveringDate(actor.agencyId(), hotelId, date);
  }

  public boolean isAgencyAuthEnforced(RequestActor actor) {
    return flags.enforceAgencyHotelAuth() && actor.role() == RequestActor.Role.AGENCY;
  }
}
