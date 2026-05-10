package com.example.backend.controller;

import com.example.backend.entity.Template;
import com.example.backend.entity.User;
import com.example.backend.repository.TemplateRepository;
import com.example.backend.repository.UserRepository;
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

    @Autowired
    private AiImageService aiImageService;

    // Injections required for the new Wizard Endpoint to save templates
    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/suggest-segments")
    public ResponseEntity<?> suggestSegments(
            @RequestParam(defaultValue = "groq") String provider,
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
            String provider = payload.getOrDefault("provider", "groq");
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

    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String prompt = payload.get("prompt");
            String provider = payload.getOrDefault("provider", "pollinations");

            if (prompt == null || prompt.trim().isEmpty()) {
                throw new RuntimeException("Image prompt is required.");
            }

            return ResponseEntity.ok(aiImageService.generateAndSaveImage(prompt, provider, userEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =====================================================================================
    // SPRINT 9: WIZARD SPECIFIC ENDPOINT
    // =====================================================================================
    @PostMapping("/wizard-generate-template")
    public ResponseEntity<?> generateWizardTemplate(@RequestBody Map<String, String> payload, @RequestHeader("X-User-Email") String userEmail) {
        try {
            String campaignName = payload.get("campaignName");
            String segmentName = payload.get("segmentName");
            String provider = payload.getOrDefault("provider", "groq");

            // 1. Build the prompt
            String prompt = "Act as an expert email marketer. Write a highly converting HTML email template for a campaign named '"
                    + campaignName + "' targeting an audience of '" + segmentName
                    + "'. Use modern inline CSS. Return ONLY valid HTML code. No markdown blocks like ```html.";

            // 2. Call your existing AI service
            String generatedHtml = aiTemplateService.generateHtmlTemplate(prompt, provider, userEmail);

            // Failsafe cleanup
            generatedHtml = generatedHtml.replace("```html", "").replace("```", "").trim();

            // 3. Fetch user to associate ownership
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. Save to database immediately so it appears in the wizard dropdown
            Template template = new Template();
            template.setName("✨ AI Generated: " + campaignName);
            template.setHtmlContent(generatedHtml);
            template.setUser(user);
            template = templateRepository.save(template);

            // 5. Return the full saved object
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}