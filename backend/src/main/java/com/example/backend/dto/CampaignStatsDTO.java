package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatsDTO {
    private Long campaignId;
    private long totalSent;
    private long uniqueOpens;
    private long uniqueClicks;
}
