package com.back.minecraftback.repository;

import com.back.minecraftback.entity.AdminUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUsersRepository extends JpaRepository<AdminUsersEntity, Long> {
    Optional<AdminUsersEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    List<AdminUsersEntity> findAllByEnabled(boolean enabled);
}
