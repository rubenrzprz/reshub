package com.rubenrzprz.reshub.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class ReservationEntity {

  @Id
  private UUID id;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "agency_id", nullable = false)
  private UUID agencyId;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "room_type_id")
  private UUID roomTypeId;

  @Column(name = "external_room_type_code", length = 64)
  private String externalRoomTypeCode;

  @Column(name = "external_room_type_name", length = 160)
  private String externalRoomTypeName;

  @Column(name = "external_ref", nullable = false, length = 64)
  private String externalRef;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "arrival_date", nullable = false)
  private LocalDate arrivalDate;

  @Column(name = "departure_date", nullable = false)
  private LocalDate departureDate;

  @Column(name = "guest_name", nullable = false, length = 160)
  private String guestName;

  @Column(name = "guest_email", length = 254)
  private String guestEmail;

  @Column(name = "guest_phone", length = 32)
  private String guestPhone;

  @Column(nullable = false)
  private Short adults;

  @Column(nullable = false)
  private Short children;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @Column(name = "cancel_reason", length = 160)
  private String cancelReason;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected ReservationEntity() {
  }

  public static ReservationEntity create(
    UUID id,
    UUID hotelId,
    UUID agencyId,
    UUID createdByUserId,
    UUID roomTypeId,
    String externalRoomTypeCode,
    String externalRoomTypeName,
    String externalRef,
    LocalDate arrivalDate,
    LocalDate departureDate,
    String guestName,
    short adults,
    short children,
    String notes
  ) {
    ReservationEntity entity = new ReservationEntity();
    entity.id = id;
    entity.hotelId = hotelId;
    entity.agencyId = agencyId;
    entity.createdByUserId = createdByUserId;
    entity.roomTypeId = roomTypeId;
    entity.externalRoomTypeCode = externalRoomTypeCode;
    entity.externalRoomTypeName = externalRoomTypeName;
    entity.externalRef = externalRef;
    entity.status = "NEW";
    entity.arrivalDate = arrivalDate;
    entity.departureDate = departureDate;
    entity.guestName = guestName;
    entity.adults = adults;
    entity.children = children;
    entity.notes = notes;
    return entity;
  }

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public UUID getId() {
    return id;
  }

  public UUID getHotelId() {
    return hotelId;
  }

  public UUID getAgencyId() {
    return agencyId;
  }

  public UUID getCreatedByUserId() {
    return createdByUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public void confirm() {
    status = "CONFIRMED";
    cancelledAt = null;
    cancelReason = null;
  }

  public void cancel(String reason) {
    status = "CANCELLED";
    cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    cancelReason = reason;
  }

  public void noShow() {
    status = "NOSHOW";
    cancelledAt = null;
    cancelReason = null;
  }
}
