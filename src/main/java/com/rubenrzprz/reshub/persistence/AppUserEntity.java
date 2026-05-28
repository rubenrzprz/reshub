package com.rubenrzprz.reshub.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUserEntity {

  @Id
  private UUID id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 72)
  private String passwordHash;

  @Column(nullable = false, length = 24)
  private String role;

  @Column(name = "hotel_id")
  private UUID hotelId;

  @Column(name = "agency_id")
  private UUID agencyId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected AppUserEntity() {
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getRole() {
    return role;
  }

  public UUID getHotelId() {
    return hotelId;
  }

  public UUID getAgencyId() {
    return agencyId;
  }
}
