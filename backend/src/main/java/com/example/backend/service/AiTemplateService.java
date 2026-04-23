package com.example.backend.service;

import com.example.backend.entity.Vault;
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

import java.util.List;
import java.util.Map;

@Service
public class AiTemplateService {

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    // Inject the Groq key from application.properties
    @Value("${groq.api.key}")
    private String groqApiKey;

    public String generateHtmlTemplate(String topic, String provider, String userEmail) {
        String prompt = "You are an expert, professional email designer. Create a highly converting, responsive HTML email template for: '" + topic + "'. "
                + "CRITICAL RULES: 1. Use modern inline CSS. 2. DO NOT include any markdown formatting like ```html. "
                + "3. Return ONLY the raw HTML code starting with <!DOCTYPE html> or <html>.";

        if ("groq".equalsIgnoreCase(provider)) {
            return callGroqApi(prompt);
        } else {
            return callGeminiApi(prompt, userEmail);
        }
    }

    public String refineHtmlTemplate(String currentHtml, String instructions, String provider, String userEmail) {
        String prompt = "Here is my current HTML email code:\n\n" + currentHtml + "\n\n"
                + "Make these changes: '" + instructions + "'. "
                + "CRITICAL RULES: 1. Keep the rest of the design intact. 2. DO NOT include markdown like ```html. "
                + "3. Return ONLY the fully updated raw HTML code.";

        if ("groq".equalsIgnoreCase(provider)) {
            return callGroqApi(prompt);
        } else {
            return callGeminiApi(prompt, userEmail);
        }
    }

    // --- THE NEW GROQ ENGINE ---
    private String callGroqApi(String prompt) {
        if (groqApiKey == null || groqApiKey.isEmpty()) throw new RuntimeException("Groq API Key is missing in properties.");

        String url = "https://api.groq.com/openai/v1/chat/completions";
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;

        try {
            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> requestMap = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(message),
                    "temperature", 0.7
            );
            requestBody = mapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Groq request body.");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey); // Groq uses Bearer token authentication

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode rootNode = mapper.readTree(response);
            String aiText = rootNode.path("choices").get(0).path("message").path("content").asText();

            return cleanMarkdown(aiText);
        } catch (Exception e) {
            throw new RuntimeException("Groq AI Error: " + e.getMessage());
        }
    }

    // --- YOUR EXISTING GEMINI ENGINE ---
    private String callGeminiApi(String prompt, String userEmail) {
        Vault vault = vaultRepository.findByUserEmail(userEmail).orElseThrow(() -> new RuntimeException("API Vault not configured."));
        String apiKey;
        try { apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim(); }
        catch (Exception e) { throw new RuntimeException("Failed to decrypt Gemini Key."); }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))));
        } catch (Exception e) { throw new RuntimeException("Failed to construct request."); }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode rootNode = mapper.readTree(response);
            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            return cleanMarkdown(aiText);
        } catch (Exception e) { throw new RuntimeException("Gemini AI Error: " + e.getMessage()); }
    }

    private String cleanMarkdown(String text) {
        if (text.startsWith("```html")) return text.replace("```html", "").replace("```", "").trim();
        if (text.startsWith("```")) return text.replace("```", "").trim();
        return text.trim();
    }
}