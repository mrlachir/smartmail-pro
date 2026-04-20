package com.example.backend.service;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.entity.Vault;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiSegmentService {

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    // THE UPGRADE: Inject SegmentRepository to read what already exists
    @Autowired
    private SegmentRepository segmentRepository;

    public String getSuggestedSegments(String userEmail) {
        // 1. DATA ISOLATION: Fetch ONLY this user's subscribers
        List<Subscriber> userSubscribers = subscriberService.getAllSubscribers(userEmail);

        if (userSubscribers.isEmpty()) {
            throw new RuntimeException("You have no subscribers. Upload a CSV first so the AI can analyze your data.");
        }

        // 2. CONTEXT ISOLATION: Find segments the user already created
        List<Segment> existingSegments = segmentRepository.findByUserEmail(userEmail);
        String existingSegmentNames = existingSegments.isEmpty() ? "None" :
                existingSegments.stream().map(Segment::getName).collect(Collectors.joining(", "));

        // 3. Extract columns and a sample of ACTUAL VALUES
        Map<String, Set<String>> columnSamples = new HashMap<>();
        columnSamples.put("status", new HashSet<>());

        for (Subscriber sub : userSubscribers) {
            columnSamples.get("status").add(sub.getStatus());

            if (sub.getCustomAttributes() != null) {
                for (Map.Entry<String, String> entry : sub.getCustomAttributes().entrySet()) {
                    columnSamples.putIfAbsent(entry.getKey(), new HashSet<>());
                    if (columnSamples.get(entry.getKey()).size() < 5) {
                        columnSamples.get(entry.getKey()).add(entry.getValue());
                    }
                }
            }
        }

        StringBuilder dataContext = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : columnSamples.entrySet()) {
            dataContext.append("- ").append(entry.getKey()).append(" (Sample values found: ").append(String.join(", ", entry.getValue())).append(")\n");
        }

        // 4. Fetch and decrypt API Key securely
        Vault vault = vaultRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("API Vault not configured. Please add your Gemini key in settings."));

        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt Gemini API Key.");
        }

        if (apiKey.isEmpty()) {
            throw new RuntimeException("Gemini API Key is empty. Update your Vault.");
        }

        // 5. Construct the strict AI Prompt (Now with Anti-Duplication Rule)
        String prompt = "You are a data-driven marketing expert. I have a subscriber database with the following columns and actual data samples:\n"
                + dataContext.toString()
                + "\nCRITICAL RULE: The user already has the following segments: [" + existingSegmentNames + "]. "
                + "DO NOT suggest these segments. You must invent 3 NEW, distinct segments based on the data provided.\n\n"
                + "You MUST return ONLY a raw JSON array of objects. Do not include any conversational text or markdown formatting (no ```json). "
                + "Each object must exactly match this structure: "
                + "{ \"name\": \"Segment Name\", \"description\": \"Why this works\", \"rules\": [ { \"column\": \"column_name\", \"operator\": \"=\", \"value\": \"target_value\" } ] }. "
                + "Valid operators are: =, !=, >, <, >=, <=.";

        // 6. Safely build the JSON request body
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            Map<String, Object> contents = Map.of("contents", List.of(parts));
            requestBody = mapper.writeValueAsString(contents);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AI request body.");
        }

//        // 7. Fire the request
//        String baseUrl = "[https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=](https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=)";
//        String url = baseUrl + apiKey;
//// 6. Fire the request (Upgraded to Gemini 2.5 Flash)
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
        String url = baseUrl + apiKey;

        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode rootNode = mapper.readTree(response);

            if (!rootNode.has("candidates")) {
                throw new RuntimeException("Google rejected the API request. Check your API Key permissions.");
            }

            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            if (aiText.startsWith("```json")) {
                aiText = aiText.replace("```json", "").replace("```", "").trim();
            } else if (aiText.startsWith("```")) {
                aiText = aiText.replace("```", "").trim();
            }

            return aiText;

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 503) {
                throw new RuntimeException("Google's AI servers are currently overloaded. Please wait 60 seconds and try again.");
            } else if (e.getStatusCode().value() == 400) {
                throw new RuntimeException("Google rejected the request format. Ensure your database contains valid data.");
            } else if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("AI Model endpoint not found. Verify the Google API URL.");
            } else {
                throw new RuntimeException("Google API Error: " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected AI Error: " + e.getMessage());
        }
    }
}