package com.example.backend.controller;

import com.example.backend.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import com.example.backend.entity.Subscriber;
import java.util.List;

@RestController
@RequestMapping("/api/subscribers")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriberController {

    @Autowired
    private SubscriberService subscriberService;

    @PostMapping("/import")
    public ResponseEntity<?> importSubscribers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please upload a valid CSV file."));
        }

        try {
            int count = subscriberService.importCsv(file);
            return ResponseEntity.ok().body(Map.of("message", "Successfully imported " + count + " subscribers."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        return ResponseEntity.ok(subscriberService.getAllSubscribers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubscriber(@PathVariable Long id) {
        try {
            subscriberService.deleteSubscriber(id);
            return ResponseEntity.ok().body(Map.of("message", "Subscriber deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error deleting subscriber."));
        }
    }
    @GetMapping("/attributes")
    public ResponseEntity<List<String>> getAvailableAttributes() {
        return ResponseEntity.ok(subscriberService.getUniqueCustomAttributes());
    }
}