package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;
    
    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;
    
    @Column(name = "interaction_type", nullable = false)
    private String interactionType; // "OPEN" or "CLICK"
    
    @Column(name = "target_url")
    private String targetUrl;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
