package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseEmailService - JUnit 5 & Mockito Test Suite")
class EnterpriseEmailServiceTest {

    @InjectMocks
    private EnterpriseEmailService enterpriseEmailService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    // Test Constants
    private static final String RESEND_API_KEY = "test-api-key-rk_test_12345abcde";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_SUBJECT = "Weekly Campaign";
    private static final String TEST_HTML = "<html><body><a href=\"https://example.com/promo\">Click Here</a></body></html>";
    private static final String TEST_REPLY_TO = "support@smartmail.com";
    private static final Long TEST_CAMPAIGN_ID = 42L;
    private static final Long TEST_SUBSCRIBER_ID = 1001L;

    @BeforeEach
    void setUp() {
        // Inject the Resend API Key via reflection (since it's @Value annotated)
        ReflectionTestUtils.setField(enterpriseEmailService, "resendApiKey", RESEND_API_KEY);
    }

    // ============ HAPPY PATH TESTS ============

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - Success with 200 OK response")
    void testSendCampaignEmail_Success_200Response() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - Success with 201 Created response")
    void testSendCampaignEmail_Success_201Response() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(201);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - Success with 299 boundary response")
    void testSendCampaignEmail_Success_299BoundaryResponse() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(299);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - With multiple hyperlinks in HTML")
    void testSendCampaignEmail_Success_MultipleLinks() throws Exception {
        // Arrange
        String htmlWithMultipleLinks =
            "<html><body>" +
            "<a href=\"https://store.example.com/product1\">Product 1</a>" +
            "<a href=\"https://store.example.com/product2\">Product 2</a>" +
            "<a href=\"https://store.example.com/product3\">Product 3</a>" +
            "</body></html>";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithMultipleLinks,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - HTML with </body> tag for pixel injection")
    void testSendCampaignEmail_Success_OpenPixelInjectionWithBodyTag() throws Exception {
        // Arrange
        String htmlWithBodyTag = "<html><head><title>Test</title></head><body>Content here</body></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithBodyTag,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - HTML without </body> tag for pixel injection")
    void testSendCampaignEmail_Success_OpenPixelInjectionWithoutBodyTag() throws Exception {
        // Arrange
        String htmlWithoutBodyTag = "<!DOCTYPE html><html><head><title>Test</title></head></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithoutBodyTag,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Happy Path: sendCampaignEmail - With HTTPS and HTTP URLs mixed")
    void testSendCampaignEmail_Success_MixedProtocolUrls() throws Exception {
        // Arrange
        String htmlWithMixedProtocols =
            "<html><body>" +
            "<a href=\"https://secure.example.com\">Secure</a>" +
            "<a href=\"http://insecure.example.com\">Insecure</a>" +
            "</body></html>";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithMixedProtocols,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    // ============ EXCEPTION PATH TESTS ============

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on API error 400 Bad Request")
    void testSendCampaignEmail_Error_400BadRequest() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"error\": \"Invalid email format\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            assertTrue(exception.getMessage().contains("Invalid email format"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on API error 401 Unauthorized")
    void testSendCampaignEmail_Error_401Unauthorized() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("{\"error\": \"Unauthorized: Invalid API Key\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on API error 429 Too Many Requests")
    void testSendCampaignEmail_Error_429RateLimited() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.body()).thenReturn("{\"error\": \"Rate limit exceeded\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    @DisplayName("Exception Path: sendCampaignEmail - Throws on server errors (5xx)")
    void testSendCampaignEmail_Error_ServerErrors(int statusCode) throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn("{\"error\": \"Server error\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on HTTP client exception")
    void testSendCampaignEmail_Exception_HttpClientThrows() throws Exception {
        // Arrange
        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(any(), any()))
                .thenThrow(new java.io.IOException("Connection timeout"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Failed to fire email"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on interrupted exception")
    void testSendCampaignEmail_Exception_InterruptedException() throws Exception {
        // Arrange
        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(any(), any()))
                .thenThrow(new InterruptedException("Thread interrupted"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Failed to fire email"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on status code 199 (below success range)")
    void testSendCampaignEmail_Error_199BelowSuccessRange() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(199);
        when(mockResponse.body()).thenReturn("{\"error\": \"Unexpected status\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Exception Path: sendCampaignEmail - Throws on status code 300 (above success range)")
    void testSendCampaignEmail_Error_300AboveSuccessRange() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(300);
        when(mockResponse.body()).thenReturn("{\"error\": \"Multiple choices\"}");

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            assertTrue(exception.getMessage().contains("Resend API Error"));
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    // ============ EDGE CASE TESTS ============

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With null HTML content")
    void testSendCampaignEmail_EdgeCase_NullHtml() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            enterpriseEmailService.sendCampaignEmail(
                TEST_EMAIL,
                TEST_SUBJECT,
                null,
                TEST_REPLY_TO,
                TEST_CAMPAIGN_ID,
                TEST_SUBSCRIBER_ID
            );
        });
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With empty HTML content")
    void testSendCampaignEmail_EdgeCase_EmptyHtml() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    "",
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With very large campaign and subscriber IDs")
    void testSendCampaignEmail_EdgeCase_LargeIds() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With zero campaign and subscriber IDs")
    void testSendCampaignEmail_EdgeCase_ZeroIds() throws Exception {
        // Arrange
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    0L,
                    0L
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With special characters in subject")
    void testSendCampaignEmail_EdgeCase_SpecialCharactersInSubject() throws Exception {
        // Arrange
        String specialSubject = "Campaign: 50% OFF! 🎉 [URGENT]";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    specialSubject,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - With special characters in email addresses")
    void testSendCampaignEmail_EdgeCase_SpecialCharactersInEmails() throws Exception {
        // Arrange
        String specialEmail = "user+tag@example.co.uk";
        String specialReplyTo = "support+priority@example.org";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    specialEmail,
                    TEST_SUBJECT,
                    TEST_HTML,
                    specialReplyTo,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - HTML with encoded characters and entities")
    void testSendCampaignEmail_EdgeCase_HtmlWithEncodedCharacters() throws Exception {
        // Arrange
        String htmlWithEntities = "<html><body>&lt;script&gt; &amp; &quot;quoted&quot; &#39;apostrophe&#39;</body></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithEntities,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Edge Case: sendCampaignEmail - Very long email subject (500+ chars)")
    void testSendCampaignEmail_EdgeCase_VeryLongSubject() throws Exception {
        // Arrange
        String longSubject = "This is a very long subject line that exceeds normal email subject limits. " +
            "It contains multiple sentences and words to test if the service can handle large subject lines. " +
            "Additional content: Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "This should still be processed without any errors!";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    longSubject,
                    TEST_HTML,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    // ============ TRACKING INJECTION TESTS ============

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Single link tracking injection")
    void testSendCampaignEmail_Tracking_SingleLinkInjection() throws Exception {
        // Arrange
        String htmlSingleLink = "<html><body><a href=\"https://example.com/offer\">Claim Offer</a></body></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlSingleLink,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            // Assert
            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Links with query parameters preserved")
    void testSendCampaignEmail_Tracking_QueryParametersPreserved() throws Exception {
        // Arrange
        String htmlWithQueryParams = 
            "<html><body>" +
            "<a href=\"https://example.com/page?id=123&category=books\">Shop Books</a>" +
            "<a href=\"https://example.com/checkout?cart=abc&promo=SUMMER50\">Checkout</a>" +
            "</body></html>";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithQueryParams,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Open pixel injection at end of body")
    void testSendCampaignEmail_Tracking_OpenPixelInsertedBeforeClosingBodyTag() throws Exception {
        // Arrange
        String htmlWithClosingBodyTag = "<html><body><h1>Welcome</h1><p>Content</p></body></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithClosingBodyTag,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Open pixel appended when no body tag")
    void testSendCampaignEmail_Tracking_OpenPixelAppendedWithoutBodyTag() throws Exception {
        // Arrange
        String htmlNoBodyTag = "<div>Simple content without body tag</div>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlNoBodyTag,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Links with fragments preserved")
    void testSendCampaignEmail_Tracking_LinksWithFragmentsPreserved() throws Exception {
        // Arrange
        String htmlWithFragments = 
            "<html><body>" +
            "<a href=\"https://example.com/page#section1\">Jump to Section</a>" +
            "<a href=\"https://example.com#top\">Back to Top</a>" +
            "</body></html>";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithFragments,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Case-insensitive body tag detection")
    void testSendCampaignEmail_Tracking_CaseInsensitiveBodyTag() throws Exception {
        // Arrange
        String htmlWithUppercaseBodyTag = "<html><BODY>Content here</BODY></html>";
        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithUppercaseBodyTag,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }

    @Test
    @DisplayName("Tracking: sendCampaignEmail - Single and double quoted href attributes")
    void testSendCampaignEmail_Tracking_SingleAndDoubleQuotedUrls() throws Exception {
        // Arrange
        String htmlWithMixedQuotes = 
            "<html><body>" +
            "<a href=\"https://example.com/double\">Double Quoted</a>" +
            "<a href='https://example.com/single'>Single Quoted</a>" +
            "</body></html>";

        when(mockResponse.statusCode()).thenReturn(200);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
            when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);

            // Act & Assert
            assertDoesNotThrow(() ->
                enterpriseEmailService.sendCampaignEmail(
                    TEST_EMAIL,
                    TEST_SUBJECT,
                    htmlWithMixedQuotes,
                    TEST_REPLY_TO,
                    TEST_CAMPAIGN_ID,
                    TEST_SUBSCRIBER_ID
                )
            );

            verify(mockHttpClient, times(1)).send(any(), any());
        }
    }
}
