package com.example.backend.controller;

import com.example.backend.entity.Subscriber;
import com.example.backend.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscribers")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriberController {

    @Autowired
    private SubscriberService subscriberService;

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
}