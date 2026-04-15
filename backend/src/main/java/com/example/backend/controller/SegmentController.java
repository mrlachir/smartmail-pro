package com.example.backend.controller;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.service.SegmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/segments")
@CrossOrigin(origins = "http://localhost:3000")
public class SegmentController {

    @Autowired
    private SegmentService segmentService;

    @PostMapping
    public ResponseEntity<Segment> createSegment(@RequestBody Segment segment) {
        return ResponseEntity.ok(segmentService.saveSegment(segment));
    }

    @GetMapping
    public ResponseEntity<List<Segment>> getAllSegments() {
        return ResponseEntity.ok(segmentService.getAllSegments());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSegment(@PathVariable Long id) {
        segmentService.deleteSegment(id);
        return ResponseEntity.ok(Map.of("message", "Segment deleted"));
    }

    // This endpoint triggers the engine to run the rules and return the matching users
    @GetMapping("/{id}/subscribers")
    public ResponseEntity<List<Subscriber>> evaluateSegment(@PathVariable Long id) {
        return ResponseEntity.ok(segmentService.getSubscribersInSegment(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Segment> updateSegment(@PathVariable Long id, @RequestBody Segment segment) {
        return ResponseEntity.ok(segmentService.updateSegment(id, segment));
    }
}