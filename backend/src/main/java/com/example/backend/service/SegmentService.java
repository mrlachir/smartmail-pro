package com.example.backend.service;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.entity.User;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.SubscriberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SegmentService {

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private UserService userService;

    public Segment saveSegment(Segment segment, String userEmail) {
        User user = userService.getOrCreateUser(userEmail);
        segment.setUser(user);
        return segmentRepository.save(segment);
    }

    public List<Segment> getAllSegments(String userEmail) {
        return segmentRepository.findByUserEmail(userEmail);
    }

    public Segment updateSegment(Long id, Segment updatedData, String userEmail) {
        Segment existingSegment = segmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        // SECURITY: Verify the user owns this segment before updating
        if (!existingSegment.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        existingSegment.setName(updatedData.getName());
        existingSegment.setDescription(updatedData.getDescription());
        existingSegment.setRules(updatedData.getRules());

        return segmentRepository.save(existingSegment);
    }

    public void deleteSegment(Long id, String userEmail) {
        segmentRepository.findById(id).ifPresent(seg -> {
            // SECURITY: Verify ownership before deleting
            if (seg.getUser().getEmail().equals(userEmail)) {
                segmentRepository.deleteById(id);
            }
        });
    }

    public List<Subscriber> getSubscribersInSegment(Long segmentId, String userEmail) {
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        if (!segment.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to segment");
        }

        // ONLY fetch subscribers that belong to THIS user
        List<Subscriber> userSubscribers = subscriberRepository.findByUserEmail(userEmail);
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rulesArray = mapper.readTree(segment.getRules());

            return userSubscribers.stream().filter(sub -> {
                if (!rulesArray.isArray() || rulesArray.isEmpty()) return true;

                for (JsonNode rule : rulesArray) {
                    String targetColumn = rule.has("column") ? rule.get("column").asText() : null;
                    String operator = rule.has("operator") ? rule.get("operator").asText() : "=";
                    String targetValue = rule.has("value") ? rule.get("value").asText() : null;

                    if (targetColumn == null || targetValue == null) continue;

                    String actualValue;
                    if (targetColumn.equalsIgnoreCase("status")) {
                        actualValue = sub.getStatus();
                    } else {
                        if (sub.getCustomAttributes() == null) return false;
                        actualValue = sub.getCustomAttributes().get(targetColumn);
                    }

                    if (actualValue == null) return false;

                    boolean rulePassed = false;
                    try {
                        double actualNum = Double.parseDouble(actualValue);
                        double targetNum = Double.parseDouble(targetValue);

                        rulePassed = switch (operator) {
                            case ">" -> actualNum > targetNum;
                            case "<" -> actualNum < targetNum;
                            case ">=" -> actualNum >= targetNum;
                            case "<=" -> actualNum <= targetNum;
                            case "!=" -> actualNum != targetNum;
                            default -> actualNum == targetNum;
                        };
                    } catch (NumberFormatException e) {
                        rulePassed = switch (operator) {
                            case "!=" -> !actualValue.equalsIgnoreCase(targetValue);
                            default -> actualValue.equalsIgnoreCase(targetValue);
                        };
                    }

                    if (!rulePassed) return false;
                }
                return true;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error parsing segment rules: " + e.getMessage());
        }
    }
}