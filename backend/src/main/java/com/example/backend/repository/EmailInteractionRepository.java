package com.example.backend.repository;

import com.example.backend.entity.EmailInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailInteractionRepository extends JpaRepository<EmailInteraction, Long> {
    boolean existsByCampaignIdAndSubscriberIdAndInteractionType(Long campaignId, Long subscriberId, String interactionType);
    long countByCampaignIdAndInteractionType(Long campaignId, String interactionType);
}
