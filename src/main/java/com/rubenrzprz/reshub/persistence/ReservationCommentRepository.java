package com.rubenrzprz.reshub.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCommentRepository extends JpaRepository<ReservationCommentEntity, UUID> {
}
