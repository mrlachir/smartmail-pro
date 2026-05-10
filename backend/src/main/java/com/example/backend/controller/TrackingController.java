package com.example.backend.controller;

import com.example.backend.entity.EmailInteraction;
import com.example.backend.repository.EmailInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/track")
@CrossOrigin(origins = "*") // Usually mail clients, no strict CORS
public class TrackingController {

    @Autowired
    private EmailInteractionRepository interactionRepository;

    private static final byte[] TRANSPARENT_GIF = java.util.Base64.getDecoder().decode("R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");

    @GetMapping("/open/{campaignId}/{subscriberId}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable Long campaignId, @PathVariable Long subscriberId) {
        EmailInteraction interaction = new EmailInteraction();
        interaction.setCampaignId(campaignId);
        interaction.setSubscriberId(subscriberId);
        interaction.setInteractionType("OPEN");
        interaction.setTimestamp(LocalDateTime.now());
        interactionRepository.save(interaction);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0L);

        return new ResponseEntity<>(TRANSPARENT_GIF, headers, HttpStatus.OK);
    }

    @GetMapping("/click/{campaignId}/{subscriberId}")
    public ResponseEntity<Void> trackClick(
            @PathVariable Long campaignId, 
            @PathVariable Long subscriberId, 
            @RequestParam("url") String targetUrl) {
            
        // IMPLIED OPEN LOGIC: If they clicked, they must have opened.
        boolean hasOpened = interactionRepository.existsByCampaignIdAndSubscriberIdAndInteractionType(campaignId, subscriberId, "OPEN");
        if (!hasOpened) {
            EmailInteraction impliedOpen = new EmailInteraction();
            impliedOpen.setCampaignId(campaignId);
            impliedOpen.setSubscriberId(subscriberId);
            impliedOpen.setInteractionType("OPEN");
            impliedOpen.setTimestamp(LocalDateTime.now());
            interactionRepository.save(impliedOpen);
        }

        // SAVE THE CLICK
        EmailInteraction clickInteraction = new EmailInteraction();
        clickInteraction.setCampaignId(campaignId);
        clickInteraction.setSubscriberId(subscriberId);
        clickInteraction.setInteractionType("CLICK");
        clickInteraction.setTargetUrl(targetUrl);
        clickInteraction.setTimestamp(LocalDateTime.now());
        interactionRepository.save(clickInteraction);

        // REDIRECT
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }
}
