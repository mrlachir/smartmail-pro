package com.example.backend.controller;

import com.example.backend.service.AiImageService;
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
    public ResponseEntity<?> suggestSegments(
            @RequestParam(defaultValue = "groq") String provider, // THE UPGRADE: Accept the provider flag
            @RequestHeader("X-User-Email") String userEmail) {
        try {
            String jsonArray = aiSegmentService.getSuggestedSegments(provider, userEmail);
            return ResponseEntity.ok(jsonArray);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/generate-template")
    public ResponseEntity<?> generateTemplate(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String topic = payload.get("topic");
            String provider = payload.getOrDefault("provider", "groq"); // Default to the faster Groq
            if (topic == null || topic.trim().isEmpty()) throw new RuntimeException("Topic required.");

            return ResponseEntity.ok(Map.of("html", aiTemplateService.generateHtmlTemplate(topic, provider, userEmail)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/refine-template")
    public ResponseEntity<?> refineTemplate(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String currentHtml = payload.get("currentHtml");
            String instructions = payload.get("instructions");
            String provider = payload.getOrDefault("provider", "groq");

            if (currentHtml == null || instructions == null) throw new RuntimeException("HTML and instructions required.");

            return ResponseEntity.ok(Map.of("html", aiTemplateService.refineHtmlTemplate(currentHtml, instructions, provider, userEmail)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    // Add this to your dependencies at the top of the class
    @Autowired
    private AiImageService aiImageService;

    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String prompt = payload.get("prompt");
            // Default to 'pollinations' if the frontend doesn't send a provider
            String provider = payload.getOrDefault("provider", "pollinations");

            if (prompt == null || prompt.trim().isEmpty()) {
                throw new RuntimeException("Image prompt is required.");
            }

            // Pass the provider flag into the engine
            return ResponseEntity.ok(aiImageService.generateAndSaveImage(prompt, provider, userEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}