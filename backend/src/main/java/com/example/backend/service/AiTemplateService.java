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
        String systemRules = "You are a premium SaaS email designer. You MUST return ONLY raw, valid HTML. "
                + "CRITICAL RULES:\n"
                + "1. NO <style> blocks. NO <div> tags. NO classes. You MUST use strict <table> layouts and inline CSS (style=\"...\") exclusively.\n"
                + "2. You MUST use this EXACT skeleton as the base for your output:\n\n"
                + "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "</head>\n"
                + "<body style=\"margin: 0; padding: 0; background-color: #f8fafc; font-family: Arial, sans-serif;\">\n"
                + "  <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f8fafc; padding:40px 0;\">\n"
                + "    <tr><td align=\"center\">\n"
                + "      <table width=\"100%\" max-width=\"600\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px; background-color:#ffffff; border-radius:8px; box-shadow:0 4px 6px rgba(0,0,0,0.05); overflow:hidden;\">\n"
                + "        \n"
                + "        <tr><td align=\"center\" style=\"padding:30px; background-color:#0f172a; color:#ffffff; font-size:24px; font-weight:bold;\">HEADER HERE</td></tr>\n"
                + "        <tr><td style=\"padding:30px; color:#334155; font-size:16px; line-height:1.6;\">CONTENT HERE</td></tr>\n"
                + "      </table>\n"
                + "    </td></tr>\n"
                + "  </table>\n"
                + "</body>\n"
                + "</html>\n\n"
                + "3. ALL buttons MUST be built as a <table> inside a <td> with a background color. Never just an <a> tag.\n"
                + "4. Make the design modern, clean, and professional.\n"
                + "Generate the requested email by populating and extending this exact skeleton. Return ONLY the HTML code. Do NOT wrap in markdown.";
        String prompt = systemRules + "\n\nNow generate a highly converting, professional email template for: '" + topic + "'.";

        String rawHtml;
        if ("groq".equalsIgnoreCase(provider)) {
            rawHtml = callGroqApi(prompt);
        } else {
            rawHtml = callGeminiApi(prompt, userEmail);
        }
        return ensureHtmlBoilerplate(rawHtml);
    }

    public String refineHtmlTemplate(String currentHtml, String instructions, String provider, String userEmail) {
        String systemRules = "You are a premium SaaS email designer. You MUST return ONLY raw, valid HTML. "
                + "CRITICAL RULES:\n"
                + "1. NO <style> blocks. NO <div> tags. NO classes. You MUST use strict <table> layouts and inline CSS (style=\"...\") exclusively.\n"
                + "2. You MUST use this EXACT skeleton as the base for your output:\n\n"
                + "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "</head>\n"
                + "<body style=\"margin: 0; padding: 0; background-color: #f8fafc; font-family: Arial, sans-serif;\">\n"
                + "  <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f8fafc; padding:40px 0;\">\n"
                + "    <tr><td align=\"center\">\n"
                + "      <table width=\"100%\" max-width=\"600\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px; background-color:#ffffff; border-radius:8px; box-shadow:0 4px 6px rgba(0,0,0,0.05); overflow:hidden;\">\n"
                + "        \n"
                + "        <tr><td align=\"center\" style=\"padding:30px; background-color:#0f172a; color:#ffffff; font-size:24px; font-weight:bold;\">HEADER HERE</td></tr>\n"
                + "        <tr><td style=\"padding:30px; color:#334155; font-size:16px; line-height:1.6;\">CONTENT HERE</td></tr>\n"
                + "      </table>\n"
                + "    </td></tr>\n"
                + "  </table>\n"
                + "</body>\n"
                + "</html>\n\n"
                + "3. ALL buttons MUST be built as a <table> inside a <td> with a background color. Never just an <a> tag.\n"
                + "4. Make the design modern, clean, and professional.\n"
                + "Generate the requested email by populating and extending this exact skeleton. Return ONLY the HTML code. Do NOT wrap in markdown.";
        String prompt = systemRules
                + "\n\nHere is the current HTML email:\n\n" + currentHtml + "\n\n"
                + "Apply these changes: '" + instructions + "'. Preserve all existing <table> structure and inline styles. "
                + "Return the fully updated raw HTML only.";

        String rawHtml;
        if ("groq".equalsIgnoreCase(provider)) {
            rawHtml = callGroqApi(prompt);
        } else {
            rawHtml = callGeminiApi(prompt, userEmail);
        }
        return ensureHtmlBoilerplate(rawHtml);
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

    private String ensureHtmlBoilerplate(String htmlContent) {
        if (htmlContent == null) return "";

        String trimmedContent = htmlContent.trim();
        if (trimmedContent.equalsIgnoreCase("null") || trimmedContent.isEmpty()) {
            return "";
        }
        
        String lowerCaseHtml = trimmedContent.toLowerCase();
        
        // If it already has the doctype or html tag, trust it and return
        if (lowerCaseHtml.startsWith("<!doctype html>") || lowerCaseHtml.startsWith("<html>")) {
            return htmlContent;
        }

        // If it's missing, wrap the LLM's raw table output in the unbreakable email shell
        return "<!DOCTYPE html>\n"
             + "<html>\n"
             + "<head>\n"
             + "  <meta charset=\"utf-8\">\n"
             + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
             + "</head>\n"
             + "<body style=\"margin: 0; padding: 0; background-color: #f8fafc; font-family: Arial, sans-serif; -webkit-font-smoothing: antialiased;\">\n"
             + "  " + htmlContent + "\n"
             + "</body>\n"
             + "</html>";
    }
}