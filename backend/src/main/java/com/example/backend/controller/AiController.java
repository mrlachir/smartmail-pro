package com.example.backend.controller;

import com.example.backend.service.AiSegmentService;
import com.example.backend.service.AiTemplateService;
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

    @Autowired
    private AiTemplateService aiTemplateService;

    @GetMapping("/suggest-segments")
    public ResponseEntity<?> suggestSegments(@RequestHeader("X-User-Email") String userEmail) {
        try {
            String jsonArray = aiSegmentService.getSuggestedSegments(userEmail);
            return ResponseEntity.ok(jsonArray);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // THE UPGRADE: Expose HTML generation
    @PostMapping("/generate-template")
    public ResponseEntity<?> generateTemplate(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String topic = payload.get("topic");
            if (topic == null || topic.trim().isEmpty()) {
                throw new RuntimeException("Topic is required.");
            }
            String rawHtml = aiTemplateService.generateHtmlTemplate(topic, userEmail);
            return ResponseEntity.ok(Map.of("html", rawHtml));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    // THE UPGRADE: Expose HTML Chatbot Refinement
    @PostMapping("/refine-template")
    public ResponseEntity<?> refineTemplate(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String currentHtml = payload.get("currentHtml");
            String instructions = payload.get("instructions");

            if (currentHtml == null || currentHtml.trim().isEmpty() || instructions == null || instructions.trim().isEmpty()) {
                throw new RuntimeException("Both current HTML and instructions are required.");
            }

            String rawHtml = aiTemplateService.refineHtmlTemplate(currentHtml, instructions, userEmail);
            return ResponseEntity.ok(Map.of("html", rawHtml));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}