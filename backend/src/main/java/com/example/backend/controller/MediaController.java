package com.example.backend.controller;

import com.example.backend.entity.Media;
import com.example.backend.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:3000")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader("X-User-Email") String userEmail) {
        try {
            Media savedMedia = mediaService.saveMedia(file, userEmail);
            return ResponseEntity.ok(savedMedia);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Media>> getGallery(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(mediaService.getUserGallery(userEmail));
    }
}