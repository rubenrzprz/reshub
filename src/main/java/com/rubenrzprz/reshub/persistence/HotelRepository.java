package com.rubenrzprz.reshub.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<HotelEntity, UUID> {
}
