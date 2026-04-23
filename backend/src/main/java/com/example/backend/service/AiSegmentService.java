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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiSegmentService {

    @Autowired private SubscriberService subscriberService;
    @Autowired private VaultRepository vaultRepository;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private SegmentRepository segmentRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    public String getSuggestedSegments(String provider, String userEmail) {
        List<Subscriber> userSubscribers = subscriberService.getAllSubscribers(userEmail);
        if (userSubscribers.isEmpty()) throw new RuntimeException("You have no subscribers. Upload a CSV first.");

        List<Segment> existingSegments = segmentRepository.findByUserEmail(userEmail);
        String existingSegmentNames = existingSegments.isEmpty() ? "None" :
                existingSegments.stream().map(Segment::getName).collect(Collectors.joining(", "));

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

        String prompt = "You are a data-driven marketing expert. Here is my database context:\n" + dataContext.toString()
                + "\nCRITICAL RULE: The user already has these segments: [" + existingSegmentNames + "]. Do not suggest these. "
                + "Invent 3 NEW distinct segments based on the data provided.\n"
                + "You MUST return ONLY a raw JSON array of objects. Do not include any conversational text or markdown formatting.\n"
                + "Structure: [ { \"name\": \"Segment Name\", \"description\": \"Why it works\", \"rules\": [ { \"column\": \"column_name\", \"operator\": \"=\", \"value\": \"target_value\" } ] } ]. "
                + "Valid operators: =, !=, >, <, >=, <=.";

        if ("groq".equalsIgnoreCase(provider)) {
            return callGroqApi(prompt);
        } else {
            return callGeminiApi(prompt, userEmail);
        }
    }

    private String callGroqApi(String prompt) {
        if (groqApiKey == null || groqApiKey.isEmpty()) throw new RuntimeException("Groq API Key is missing in properties.");

        String url = "https://api.groq.com/openai/v1/chat/completions";
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;

        try {
            requestBody = mapper.writeValueAsString(Map.of(
                    "model", "llama-3.1-8b-instant",

                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.7
            ));
        } catch (Exception e) { throw new RuntimeException("Failed to construct Groq request body."); }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), String.class);
            return cleanJson(mapper.readTree(response).path("choices").get(0).path("message").path("content").asText());
        } catch (Exception e) { throw new RuntimeException("Groq API Error: " + e.getMessage()); }
    }

    private String callGeminiApi(String prompt, String userEmail) {
        Vault vault = vaultRepository.findByUserEmail(userEmail).orElseThrow(() -> new RuntimeException("API Vault not configured."));
        String apiKey;
        try { apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim(); }
        catch (Exception e) { throw new RuntimeException("Failed to decrypt Gemini API Key."); }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;

        try {
            requestBody = mapper.writeValueAsString(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))));
        } catch (Exception e) { throw new RuntimeException("Failed to construct AI request body."); }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), String.class);
            return cleanJson(mapper.readTree(response).path("candidates").get(0).path("content").path("parts").get(0).path("text").asText());
        } catch (Exception e) { throw new RuntimeException("Gemini API Error: " + e.getMessage()); }
    }

    private String cleanJson(String text) {
        if (text.startsWith("```json")) return text.replace("```json", "").replace("```", "").trim();
        if (text.startsWith("```")) return text.replace("```", "").trim();
        return text.trim();
    }
}