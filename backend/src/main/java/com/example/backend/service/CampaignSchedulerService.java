package com.example.backend.service;

import com.example.backend.entity.Campaign;
import com.example.backend.entity.Subscriber;
import com.example.backend.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignSchedulerService {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private EnterpriseEmailService emailService;

    @Scheduled(fixedRate = 60000) // Wakes up every 60 seconds
    @Transactional
    public void checkAndLaunchScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<Campaign> pending = campaignRepository.findByStatusAndScheduledAtBefore("SCHEDULED", now);

        for (Campaign campaign : pending) {
            System.out.println("⏰ Scheduled time hit for: " + campaign.getName());
            
            try {
                // 1. Set status to prevent double-firing
                campaign.setStatus("SENDING");
                campaignRepository.save(campaign);

                // 2. Blast emails
                int count = 0;
                for (Subscriber sub : campaign.getSegment().getSubscribers()) {
                    emailService.sendCampaignEmail(
                        sub.getEmail(), 
                        campaign.getSubject(), 
                        campaign.getTemplate().getHtmlContent(), 
                        campaign.getUserEmail(),
                        campaign.getId(),
                        sub.getId()
                    );
                    count++;
                }

                // 3. Finalize
                campaign.setStatus("SENT");
                campaign.setSentAt(LocalDateTime.now());
                campaignRepository.save(campaign);
                System.out.println("✅ Successfully processed scheduled campaign. Sent: " + count);

            } catch (Exception e) {
                campaign.setStatus("FAILED");
                campaignRepository.save(campaign);
                System.err.println("❌ Scheduled launch failed: " + e.getMessage());
            }
        }
    }
}
