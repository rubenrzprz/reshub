package com.rubenrzprz.reshub.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgencyHotelAuthRepository extends JpaRepository<AgencyHotelAuthEntity, UUID> {

  @Query("""
    select count(a) > 0
    from AgencyHotelAuthEntity a
    where a.agencyId = :agencyId
      and a.hotelId = :hotelId
      and a.status = 'ACTIVE'
      and (a.validFrom is null or a.validFrom <= :arrivalDate)
      and (a.validTo is null or a.validTo >= :arrivalDate)
    """)
  boolean existsActiveCoveringDate(
    @Param("agencyId") UUID agencyId,
    @Param("hotelId") UUID hotelId,
    @Param("arrivalDate") LocalDate arrivalDate
  );
}
