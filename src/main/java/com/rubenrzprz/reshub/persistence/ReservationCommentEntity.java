package com.rubenrzprz.reshub.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "reservation_comment")
public class ReservationCommentEntity {

  @Id
  private UUID id;

  @Column(name = "reservation_id", nullable = false)
  private UUID reservationId;

  @Column(name = "author_user_id", nullable = false)
  private UUID authorUserId;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  protected ReservationCommentEntity() {
  }

  public ReservationCommentEntity(UUID id, UUID reservationId, UUID authorUserId, String body) {
    this.id = id;
    this.reservationId = reservationId;
    this.authorUserId = authorUserId;
    this.body = body;
  }

  @PrePersist
  void prePersist() {
    createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public UUID getId() {
    return id;
  }

  public UUID getReservationId() {
    return reservationId;
  }

  public UUID getAuthorUserId() {
    return authorUserId;
  }

  public String getBody() {
    return body;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
