package com.rubenrzprz.reshub.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hotel")
public class HotelEntity {

  @Id
  private UUID id;

  @Column(nullable = false, length = 24)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected HotelEntity() {
  }

  public UUID getId() {
    return id;
  }
}
