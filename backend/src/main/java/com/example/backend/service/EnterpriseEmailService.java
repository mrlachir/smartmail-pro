//package com.example.backend.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class EnterpriseEmailService {
//
//    @Value("${resend.api.key}")
//    private String resendApiKey;
//
//    public void sendCampaignEmail(String toEmail, String subject, String htmlContent, String replyToEmail, Long campaignId, Long subscriberId) {
//        try {
//            htmlContent = injectTracking(htmlContent, campaignId, subscriberId);
//
//            // 1. Construct the exact JSON payload expected by Resend
//            Map<String, Object> payload = new HashMap<>();
//// The professional sender identity
//            payload.put("from", "Smartmail Pro <noreply@tawsilfree.com>");            payload.put("to", List.of(toEmail));
//            payload.put("subject", subject);
//            payload.put("html", htmlContent);
//            payload.put("reply_to", replyToEmail); // This is where replies will go
//
//            ObjectMapper mapper = new ObjectMapper();
//            String jsonBody = mapper.writeValueAsString(payload);
//
//            // 2. Fire the payload via native Java HttpClient
//            HttpClient client = HttpClient.newHttpClient();
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://api.resend.com/emails"))
//                    .header("Authorization", "Bearer " + resendApiKey)
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                    .build();
//
//            // 3. Await the response
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() < 200 || response.statusCode() >= 300) {
//                throw new RuntimeException("Resend API Error: " + response.body());
//            }
//
//            System.out.println("✅ Campaign payload successfully delivered to Resend for: " + toEmail);
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to fire email: " + e.getMessage());
//        }
//    }
//
//    private String injectTracking(String html, Long campaignId, Long subscriberId) {
//        // THE EXACT BASE URL
////       String baseUrl = "https://d14663f39f8767.lhr.life/api/track";
//        String baseUrl = "http://localhost:8080/api/track";
//
//        // 1. Rewrite Links (Regex to find href="http...")
//        String linkRegex = "href=[\"'](http[s]?://[^\"']+)[\"']";
//        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(linkRegex).matcher(html);
//        StringBuilder sb = new StringBuilder();
//
//        while (matcher.find()) {
//            String originalUrl = matcher.group(1);
//            String encodedUrl = java.net.URLEncoder.encode(originalUrl, java.nio.charset.StandardCharsets.UTF_8);
//            String trackingUrl = baseUrl + "/click/" + campaignId + "/" + subscriberId + "?url=" + encodedUrl;
//            matcher.appendReplacement(sb, "href=\"" + trackingUrl + "\"");
//        }
//        matcher.appendTail(sb);
//        html = sb.toString();
//
//        // 2. Inject Open Pixel (Guaranteed to have /api/track/open/...)
//        String pixel = "<img src=\"" + baseUrl + "/open/" + campaignId + "/" + subscriberId + "\" width=\"1\" height=\"1\" alt=\"\" style=\"display:none;\" />";
//        if (html.toLowerCase().contains("</body>")) {
//            html = html.replaceAll("(?i)</body>", pixel + "\n</body>");
//        } else {
//            html += "\n" + pixel;
//        }
//
//        return html;
//    }
//}*

package com.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EnterpriseEmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void sendCampaignEmail(String toEmail, String subject, String htmlContent, String replyToEmail, Long campaignId, Long subscriberId) {
        try {
            htmlContent = injectTracking(htmlContent, campaignId, subscriberId);

            // 1. Construct the exact JSON payload expected by Resend
            Map<String, Object> payload = new HashMap<>();
            // The professional sender identity
            payload.put("from", "Smartmail Pro <noreply@tawsilfree.com>");
            payload.put("to", List.of(toEmail));
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
                throw new IllegalStateException("Resend API Error: " + response.body());
            }

            log.info("✅ Campaign payload successfully delivered to Resend for: {}", toEmail);

        } catch (InterruptedException e) {
            // CRITICAL: Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Email sending process was interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fire email: " + e.getMessage(), e);
        }
    }

    private String injectTracking(String html, Long campaignId, Long subscriberId) {
        // THE EXACT BASE URL
        String baseUrl = "http://localhost:8080/api/track";

        // 1. Rewrite Links (Regex to find href="http...")
        String linkRegex = "href=[\"'](https?://[^\"']+)[\"']";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(linkRegex).matcher(html);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            String encodedUrl = java.net.URLEncoder.encode(originalUrl, java.nio.charset.StandardCharsets.UTF_8);
            String trackingUrl = baseUrl + "/click/" + campaignId + "/" + subscriberId + "?url=" + encodedUrl;
            matcher.appendReplacement(sb, "href=\"" + trackingUrl + "\"");
        }
        matcher.appendTail(sb);
        html = sb.toString();

        // 2. Inject Open Pixel (Guaranteed to have /api/track/open/...)
        String pixel = "<img src=\"" + baseUrl + "/open/" + campaignId + "/" + subscriberId + "\" width=\"1\" height=\"1\" alt=\"\" style=\"display:none;\" />";
        if (html.toLowerCase().contains("</body>")) {
            html = html.replaceAll("(?i)</body>", pixel + "\n</body>");
        } else {
            html += "\n" + pixel;
        }

        return html;
    }
}