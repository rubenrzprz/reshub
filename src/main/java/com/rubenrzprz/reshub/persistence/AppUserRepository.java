package com.rubenrzprz.reshub.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {
  List<AppUserEntity> findByEmailIgnoreCase(String email);
}
