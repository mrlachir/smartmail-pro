package com.example.backend.controller;

import com.example.backend.entity.Subscriber;
import com.example.backend.service.SubscriberService;
import com.example.backend.repository.EmailInteractionRepository;
import com.example.backend.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/subscribers")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriberController {

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private EmailInteractionRepository interactionRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, @RequestHeader("X-User-Email") String userEmail) {
        try {
            int count = subscriberService.importCsv(file, userEmail);
            return ResponseEntity.ok(Map.of("message", "Successfully imported " + count + " subscribers."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Subscriber>> getAllSubscribers(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(subscriberService.getAllSubscribers(userEmail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubscriber(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        try {
            subscriberService.deleteSubscriber(id, userEmail);
            return ResponseEntity.ok().body(Map.of("message", "Subscriber deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error deleting subscriber."));
        }
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<String>> getAvailableAttributes(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(subscriberService.getUniqueCustomAttributes(userEmail));
    }

    @GetMapping("/top-engaged")
    public ResponseEntity<?> getTopEngagedSubscribers(@RequestHeader("X-User-Email") String userEmail) {
        try {
            // Fetch top 5 subscriber IDs and their counts
            List<Object[]> topData = interactionRepository.findTopSubscriberIdsWithCount(PageRequest.of(0, 5));
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : topData) {
                Long subId = (Long) row[0];
                Long count = (Long) row[1];
                
                subscriberRepository.findById(subId).ifPresent(sub -> {
                    // Only return if they belong to this user's segment/account
                    if (sub.getUser() != null && sub.getUser().getEmail().equals(userEmail)) {
                        result.add(Map.of(
                            "email", sub.getEmail(),
                            "interactions", count,
                            "status", sub.getStatus() != null ? sub.getStatus() : "SUBSCRIBED"
                        ));
                    }
                });
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}