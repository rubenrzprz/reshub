package com.rubenrzprz.reshub.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agency_hotel_auth")
public class AgencyHotelAuthEntity {

  @Id
  private UUID id;

  @Column(name = "agency_id", nullable = false)
  private UUID agencyId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(nullable = false, length = 12)
  private String status;

  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected AgencyHotelAuthEntity() {
  }

  public UUID getId() {
    return id;
  }
}
