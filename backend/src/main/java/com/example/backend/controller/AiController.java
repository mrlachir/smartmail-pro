package com.example.backend.controller;

import com.example.backend.service.AiSegmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AiController {

    @Autowired
    private AiSegmentService aiSegmentService;

    @GetMapping("/suggest-segments")
    public ResponseEntity<?> suggestSegments(@RequestHeader("X-User-Email") String userEmail) {
        try {
            String jsonArray = aiSegmentService.getSuggestedSegments(userEmail);
            return ResponseEntity.ok(jsonArray);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}