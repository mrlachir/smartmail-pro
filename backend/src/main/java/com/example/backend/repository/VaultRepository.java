package com.example.backend.repository;

import com.example.backend.entity.Vault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultRepository extends JpaRepository<Vault, Long> {

    // Spring automatically turns this into: SELECT * FROM vault WHERE user_id = ?
    Vault findByUserId(Long userId);
}