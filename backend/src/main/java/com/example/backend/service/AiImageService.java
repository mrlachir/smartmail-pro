package com.example.backend.service;

import com.example.backend.entity.Media;
import com.example.backend.entity.Vault;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiImageService {

    private final MediaService mediaService;
    private final VaultRepository vaultRepository;
    private final EncryptionUtil encryptionUtil;

    // Constructor Injection
    public AiImageService(MediaService mediaService, VaultRepository vaultRepository, EncryptionUtil encryptionUtil) {
        this.mediaService = mediaService;
        this.vaultRepository = vaultRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public Media generateAndSaveImage(String prompt, String provider, String userEmail) {
        // Route the request based on the UI toggle
        if ("gemini".equalsIgnoreCase(provider)) {
            return generateWithGemini(prompt, userEmail);
        } else {
            return generateWithPollinations(prompt, userEmail);
        }
    }

    private Media generateWithPollinations(String prompt, String userEmail) {
        try {
            String url = UriComponentsBuilder.fromUriString("https://image.pollinations.ai/prompt/{prompt}")
                    .queryParam("width", "800")
                    .queryParam("height", "600")
                    .queryParam("nologo", "true")
                    .buildAndExpand(prompt)
                    .encode()
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Free AI returned empty data.");
            }

            return mediaService.saveMediaFromBytes(imageBytes, "pollinations_ai.jpg", "image/jpeg", userEmail);

        } catch (Exception e) {
            throw new RuntimeException("Free AI Image Error: " + e.getMessage());
        }
    }

    private Media generateWithGemini(String prompt, String userEmail) {
        Vault vault = vaultRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("API Vault not configured."));

        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(vault.getGeminiApiKeyEncrypted()).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt Gemini API Key.");
        }

        if (apiKey.isEmpty()) throw new RuntimeException("API Key is empty.");

        String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=";
        String url = baseUrl + apiKey;

        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> partContainer = Map.of("parts", List.of(textPart));
            Map<String, Object> generationConfig = Map.of("responseModalities", List.of("TEXT", "IMAGE"));

            Map<String, Object> requestMap = Map.of(
                    "contents", List.of(partContainer),
                    "generationConfig", generationConfig
            );
            requestBody = mapper.writeValueAsString(requestMap);
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

            if (!rootNode.has("candidates")) {
                throw new RuntimeException("Google rejected the request.");
            }

            JsonNode partsArray = rootNode.path("candidates").get(0).path("content").path("parts");
            String base64Image = null;
            String mimeType = "image/png";

            for (JsonNode part : partsArray) {
                if (part.has("inlineData")) {
                    base64Image = part.path("inlineData").path("data").asText();
                    mimeType = part.path("inlineData").path("mimeType").asText("image/png");
                    break;
                }
            }

            if (base64Image == null || base64Image.isEmpty()) {
                throw new RuntimeException("Google AI returned empty image.");
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return mediaService.saveMediaFromBytes(imageBytes, "gemini_image.png", mimeType, userEmail);

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("API Quota Exceeded. Image generation requires a paid Google AI Studio tier.");
            } else {
                throw new RuntimeException("Google API Error: " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new RuntimeException("AI Image Error: " + e.getMessage());
        }
    }
}