package com.example.backend.service;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
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

    public Segment saveSegment(Segment segment) {
        return segmentRepository.save(segment);
    }

    public List<Segment> getAllSegments() {
        return segmentRepository.findAll();
    }

    public void deleteSegment(Long id) {
        if(segmentRepository.existsById(id)) {
            segmentRepository.deleteById(id);
        }
    }
    public Segment updateSegment(Long id, Segment updatedData) {
        Segment existingSegment = segmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        existingSegment.setName(updatedData.getName());
        existingSegment.setDescription(updatedData.getDescription());
        existingSegment.setRules(updatedData.getRules());

        return segmentRepository.save(existingSegment);
    }

    public List<Subscriber> getSubscribersInSegment(Long segmentId) {
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment not found"));

        List<Subscriber> allSubscribers = subscriberRepository.findAll();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rulesArray = mapper.readTree(segment.getRules());

            return allSubscribers.stream().filter(sub -> {
                // If it's not an array, or it's empty, everyone passes
                if (!rulesArray.isArray() || rulesArray.isEmpty()) return true;

                // Evaluate EVERY rule in the array (AND logic)
                for (JsonNode rule : rulesArray) {
                    String targetColumn = rule.has("column") ? rule.get("column").asText() : null;
                    String operator = rule.has("operator") ? rule.get("operator").asText() : "=";
                    String targetValue = rule.has("value") ? rule.get("value").asText() : null;

                    if (targetColumn == null || targetValue == null) continue;

                    String actualValue;
                    if (targetColumn.equalsIgnoreCase("status")) {
                        actualValue = sub.getStatus();
                    } else {
                        // Safely check custom attributes
                        if (sub.getCustomAttributes() == null) return false;
                        actualValue = sub.getCustomAttributes().get(targetColumn);
                    }

                    // If the subscriber doesn't have this column at all, they fail this rule
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

                    // If they fail even ONE rule, filter them out immediately
                    if (!rulePassed) {
                        return false;
                    }
                }

                // If they survived the loop, they passed all rules
                return true;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error parsing segment rules: " + e.getMessage());
        }
    }

}