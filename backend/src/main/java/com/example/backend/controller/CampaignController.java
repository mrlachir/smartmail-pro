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

            // 1. Fetch relations
            Segment segment = segmentRepository.findById(segmentId)
                    .orElseThrow(() -> new RuntimeException("Segment not found"));
            Template template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found"));

            // 2. Create the Campaign Record
            Campaign campaign = new Campaign();
            campaign.setName(name);
            campaign.setSubject(subject);
            campaign.setUserEmail(userEmail);
            campaign.setSegment(segment);
            campaign.setTemplate(template);
            campaign.setStatus("SENDING");
            campaign = campaignRepository.save(campaign);

            // 3. Execute the Blast
            // NOTE: Replace this mock email array with your actual segment.getSubscribers() logic
            // once you connect your specific Subscriber mapping to Segments.
            // For this test, we fire it to your own email so you can see it work.
//            String[] targetEmails = { userEmail };
//
//            int sentCount = 0;
//            for (String email : targetEmails) {
//                try {
//                    emailService.sendCampaignEmail(email, subject, template.getHtmlContent(), userEmail);
//                    sentCount++;
//                } catch (Exception e) {
//                    // UPGRADE: Print the exact reason Resend rejected it
//                    System.err.println("Failed to send to: " + email + " | Reason: " + e.getMessage());
//                }
//            }
            // 3. Execute the Blast to your verified recipients
            // 3. THE SANDBOX OVERRIDE
            // We are forcing the target to the master account to bypass the 403 Error
            // 3. Execute the Blast dynamically based on the Segment
            List<String> targetEmails = segment.getSubscribers().stream()
                    .map(sub -> sub.getEmail())
                    .toList();

            int sentCount = 0;
            for (String email : targetEmails) {
                try {
                    emailService.sendCampaignEmail(email, subject, template.getHtmlContent(), userEmail);
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("❌ Delivery Failed for: " + email + " | Reason: " + e.getMessage());
                }
            }

            // 4. Finalize
            campaign.setStatus(sentCount > 0 ? "SENT" : "FAILED");
            campaign.setSentAt(LocalDateTime.now());
            campaignRepository.save(campaign);

            return ResponseEntity.ok(Map.of("message", "Campaign launched! Sent to " + sentCount + " recipients."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}