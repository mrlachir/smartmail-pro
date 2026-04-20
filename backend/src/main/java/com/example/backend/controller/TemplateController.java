package com.example.backend.controller;

import com.example.backend.entity.Template;
import com.example.backend.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "http://localhost:3000")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(templateService.getAllTemplates(userEmail));
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody Template template, @RequestHeader("X-User-Email") String userEmail) {
        try {
            return ResponseEntity.ok(templateService.saveTemplate(template, userEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody Template template, @RequestHeader("X-User-Email") String userEmail) {
        try {
            return ResponseEntity.ok(templateService.updateTemplate(id, template, userEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        templateService.deleteTemplate(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Template deleted successfully"));
    }
}