package com.example.backend.repository;

import com.example.backend.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}