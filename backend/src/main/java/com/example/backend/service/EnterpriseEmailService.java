package com.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnterpriseEmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void sendCampaignEmail(String toEmail, String subject, String htmlContent, String replyToEmail) {
        try {
            // 1. Construct the exact JSON payload expected by Resend
            Map<String, Object> payload = new HashMap<>();
// The professional sender identity
            payload.put("from", "Smartmail Pro <noreply@tawsilfree.com>");            payload.put("to", List.of(toEmail));
            payload.put("subject", subject);
            payload.put("html", htmlContent);
            payload.put("reply_to", replyToEmail); // This is where replies will go

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(payload);

            // 2. Fire the payload via native Java HttpClient
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 3. Await the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Resend API Error: " + response.body());
            }

            System.out.println("✅ Campaign payload successfully delivered to Resend for: " + toEmail);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fire email: " + e.getMessage());
        }
    }
}