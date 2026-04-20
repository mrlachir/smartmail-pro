package com.example.backend.service;

import com.example.backend.entity.Vault;
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

import java.util.List;
import java.util.Map;

@Service
public class AiTemplateService {

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    public String generateHtmlTemplate(String topic, String userEmail) {
        // 1. Authenticate API Key
        Vault vault = vaultRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("API Vault not configured. Please add your Gemini key in settings."));

        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt Gemini API Key.");
        }

        if (apiKey.isEmpty()) throw new RuntimeException("API Key is empty.");

        // 2. Strict HTML Prompt
        String prompt = "You are an expert, professional email designer. Create a highly converting, responsive HTML email template for the following campaign context: '" + topic + "'. "
                + "CRITICAL RULES: "
                + "1. Use modern, clean inline CSS (standard for email clients). "
                + "2. Include placeholder text (Lorem Ipsum) or relevant placeholder copy. "
                + "3. DO NOT include any markdown formatting like ```html. "
                + "4. Return ONLY the raw, pure HTML code starting with <!DOCTYPE html> or <html>.";

        // 3. Prepare Request
//        String baseUrl = "[https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=](https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=)";
//        String url = baseUrl + apiKey;
        String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
        String url = baseUrl + apiKey;


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

        // 4. Fire Request
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode rootNode = mapper.readTree(response);

            if (!rootNode.has("candidates")) {
                throw new RuntimeException("Google rejected the API request.");
            }

            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Strip residual markdown just in case
            if (aiText.startsWith("```html")) {
                aiText = aiText.replace("```html", "").replace("```", "").trim();
            } else if (aiText.startsWith("```")) {
                aiText = aiText.replace("```", "").trim();
            }

            return aiText;

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 503) {
                throw new RuntimeException("Google's AI servers are overloaded. Please try again in 60 seconds.");
            } else {
                throw new RuntimeException("Google API Error: " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected AI Error: " + e.getMessage());
        }
    }

    // THE UPGRADE: Iterative Chatbot Refinement
    public String refineHtmlTemplate(String currentHtml, String instructions, String userEmail) {
        Vault vault = vaultRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("API Vault not configured."));

        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt Gemini API Key.");
        }

        if (apiKey.isEmpty()) throw new RuntimeException("API Key is empty.");

        // Instruct Gemini to modify the existing code
        String prompt = "You are an expert email developer. Here is my current HTML email code:\n\n"
                + currentHtml + "\n\n"
                + "The user wants you to make the following changes: '" + instructions + "'. "
                + "CRITICAL RULES: "
                + "1. Apply the changes perfectly while keeping the rest of the design intact. "
                + "2. Maintain inline CSS. "
                + "3. DO NOT include any markdown formatting like ```html. "
                + "4. Return ONLY the fully updated raw HTML code starting with <!DOCTYPE html> or <html>.";

        String baseUrl = "[https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=](https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=)";
        String url = baseUrl + apiKey;

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

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode rootNode = mapper.readTree(response);
            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            if (aiText.startsWith("```html")) {
                aiText = aiText.replace("```html", "").replace("```", "").trim();
            } else if (aiText.startsWith("```")) {
                aiText = aiText.replace("```", "").trim();
            }
            return aiText;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 503) {
                throw new RuntimeException("Google's AI servers are overloaded. Please try again in 60 seconds.");
            } else {
                throw new RuntimeException("Google API Error: " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected AI Error: " + e.getMessage());
        }
    }
}