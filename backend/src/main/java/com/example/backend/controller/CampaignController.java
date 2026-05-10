package com.example.backend.controller;

import com.example.backend.entity.Campaign;
import com.example.backend.entity.Segment;
import com.example.backend.entity.Template;
import com.example.backend.repository.CampaignRepository;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.TemplateRepository;
import com.example.backend.service.EnterpriseEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.example.backend.entity.Subscriber;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "http://localhost:3000")
public class CampaignController {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private SegmentRepository segmentRepository;
    @Autowired private TemplateRepository templateRepository;
    @Autowired private EnterpriseEmailService emailService;

    @GetMapping
    public ResponseEntity<List<Campaign>> getCampaigns(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(campaignRepository.findByUserEmailOrderByCreatedAtDesc(userEmail));
    }

    @PostMapping("/launch")
    public ResponseEntity<?> launchCampaign(@RequestHeader("X-User-Email") String userEmail, @RequestBody Map<String, Object> payload) {
        try {
            Long segmentId = Long.valueOf(payload.get("segmentId").toString());
            Long templateId = Long.valueOf(payload.get("templateId").toString());
            String name = payload.get("name").toString();
            String subject = payload.get("subject").toString();
            
            // Extract the new Scheduled Date
            String scheduledAtStr = payload.get("scheduledAt") != null ? payload.get("scheduledAt").toString() : null;
            LocalDateTime scheduledAt = null;
            if (scheduledAtStr != null && !scheduledAtStr.trim().isEmpty()) {
                scheduledAt = LocalDateTime.parse(scheduledAtStr);
            }

            Segment segment = segmentRepository.findById(segmentId).orElseThrow(() -> new RuntimeException("Segment not found"));
            Template template = templateRepository.findById(templateId).orElseThrow(() -> new RuntimeException("Template not found"));

            Campaign campaign = new Campaign();
            campaign.setName(name);
            campaign.setSubject(subject);
            campaign.setUserEmail(userEmail);
            campaign.setSegment(segment);
            campaign.setTemplate(template);
            campaign.setScheduledAt(scheduledAt);

            // LOGIC: If it has a future date, just save it and STOP. 
            if (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())) {
                campaign.setStatus("SCHEDULED");
                campaignRepository.save(campaign);
                return ResponseEntity.ok(Map.of("message", "Campaign scheduled to launch at " + scheduledAt.toString()));
            }

            // LOGIC: If no date or past date, fire immediately
            campaign.setStatus("SENDING");
            campaign = campaignRepository.save(campaign);

            // Execute blast
            List<String> targetEmails = segment.getSubscribers().stream().map(Subscriber::getEmail).toList();
            int sentCount = 0;
            for (String email : targetEmails) {
                try {
                    emailService.sendCampaignEmail(email, subject, template.getHtmlContent(), userEmail);
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("❌ Delivery Failed for: " + email + " | Reason: " + e.getMessage());
                }
            }

            campaign.setStatus(sentCount > 0 ? "SENT" : "FAILED");
            campaign.setSentAt(LocalDateTime.now());
            campaignRepository.save(campaign);

            return ResponseEntity.ok(Map.of("message", "Campaign launched immediately! Sent to " + sentCount + " recipients."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}