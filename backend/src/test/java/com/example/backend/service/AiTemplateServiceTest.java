package com.example.backend.service;

import com.example.backend.entity.Vault;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiTemplateService - JUnit 5 & Mockito Test Suite")
class AiTemplateServiceTest {

    @InjectMocks
    private AiTemplateService aiTemplateService;

    @Mock
    private VaultRepository vaultRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    // Test Constants
    private static final String GROQ_API_KEY = "gsk_test_1234567890abcdefghijklmnop";
    private static final String TEST_TOPIC = "Summer Promotions Campaign";
    private static final String TEST_USER_EMAIL = "user@example.com";
    private static final String GROQ_PROVIDER = "groq";
    private static final String GEMINI_PROVIDER = "gemini";
    private static final String TEST_HTML = "<html><body><table><tr><td>Content</td></tr></table></body></html>";
    private static final String TEST_INSTRUCTIONS = "Change the background color to blue";
    private static final String GEMINI_API_KEY = "AIzaSy_test_key_1234567890";

    @BeforeEach
    void setUp() {
        // Inject the Groq API Key via reflection
        ReflectionTestUtils.setField(aiTemplateService, "groqApiKey", GROQ_API_KEY);
    }

    // ============ generateHtmlTemplate HAPPY PATH TESTS ============

    @Test
    @DisplayName("Happy Path: generateHtmlTemplate - Success with Groq provider")
    void testGenerateHtmlTemplate_Success_WithGroqProvider() {
        // Arrange
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<!DOCTYPE html><html><body><table><tr><td>Generated Email</td></tr></table></body></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("<!DOCTYPE html>") || result.contains("<html>"));
            assertTrue(result.contains("Generated Email"));
        }
    }

    @Test
    @DisplayName("Happy Path: generateHtmlTemplate - Success with Gemini provider")
    void testGenerateHtmlTemplate_Success_WithGeminiProvider() throws Exception {
        // Arrange
        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<!DOCTYPE html><html><body>Gemini Generated Email</body></html>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Gemini Generated Email") || result.contains("<html>"));
        }
    }

    @Test
    @DisplayName("Happy Path: generateHtmlTemplate - Groq response with markdown code blocks")
    void testGenerateHtmlTemplate_Success_GroqResponseWithMarkdown() {
        // Arrange
        String groqResponseWithMarkdown = "{\"choices\":[{\"message\":{\"content\":\"```html\\n<!DOCTYPE html><html><body>Email</body></html>\\n```\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponseWithMarkdown);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertFalse(result.contains("```"));
            assertTrue(result.contains("<!DOCTYPE html>") || result.contains("<html>"));
        }
    }

    @Test
    @DisplayName("Happy Path: generateHtmlTemplate - Gemini response without HTML wrapper needs boilerplate")
    void testGenerateHtmlTemplate_Success_GeminiRawHtmlWithBoilerplate() throws Exception {
        // Arrange
        String geminiRawResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<table><tr><td>Raw HTML without DOCTYPE</td></tr></table>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiRawResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.startsWith("<!DOCTYPE html>"));
            assertTrue(result.contains("<html>"));
        }
    }

    // ============ generateHtmlTemplate EXCEPTION PATH TESTS ============

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Groq API key is null")
    void testGenerateHtmlTemplate_Exception_GroqApiKeyNull() {
        // Arrange
        ReflectionTestUtils.setField(aiTemplateService, "groqApiKey", null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Groq API Key is missing"));
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Groq API key is empty")
    void testGenerateHtmlTemplate_Exception_GroqApiKeyEmpty() {
        // Arrange
        ReflectionTestUtils.setField(aiTemplateService, "groqApiKey", "");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Groq API Key is missing"));
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Groq REST call throws RestClientException")
    void testGenerateHtmlTemplate_Exception_GroqRestCallFails() {
        // Arrange
        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class)))
                            .thenThrow(new RestClientException("Connection timeout"));
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Groq AI Error"));
        }
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Vault not found for Gemini")
    void testGenerateHtmlTemplate_Exception_VaultNotFound() {
        // Arrange
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("API Vault not configured"));
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Decryption fails for Gemini API key")
    void testGenerateHtmlTemplate_Exception_DecryptionFailsForGemini() throws Exception {
        // Arrange
        Vault mockVault = new Vault();
        mockVault.setGeminiApiKeyEncrypted("encrypted_key_xyz");
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(mockVault));
        when(encryptionUtil.decrypt("encrypted_key_xyz")).thenThrow(new RuntimeException("Decryption failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Failed to decrypt Gemini Key"));
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - Gemini REST call throws exception")
    void testGenerateHtmlTemplate_Exception_GeminiRestCallFails() throws Exception {
        // Arrange
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class)))
                            .thenThrow(new RestClientException("API unreachable"));
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Gemini AI Error"));
        }
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - JSON parsing error in Groq response")
    void testGenerateHtmlTemplate_Exception_GroqJsonParsingError() {
        // Arrange
        String malformedJson = "{invalid json}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(malformedJson);
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Groq AI Error"));
        }
    }

    @Test
    @DisplayName("Exception Path: generateHtmlTemplate - JSON parsing error in Gemini response")
    void testGenerateHtmlTemplate_Exception_GeminiJsonParsingError() throws Exception {
        // Arrange
        String malformedJson = "{invalid response}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(malformedJson);
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Gemini AI Error"));
        }
    }

    // ============ refineHtmlTemplate HAPPY PATH TESTS ============

    @Test
    @DisplayName("Happy Path: refineHtmlTemplate - Success with Groq provider")
    void testRefineHtmlTemplate_Success_WithGroqProvider() {
        // Arrange
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<!DOCTYPE html><html><body><table><tr><td>Refined Email</td></tr></table></body></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Refined Email") || result.contains("<html>"));
        }
    }

    @Test
    @DisplayName("Happy Path: refineHtmlTemplate - Success with Gemini provider")
    void testRefineHtmlTemplate_Success_WithGeminiProvider() throws Exception {
        // Arrange
        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<!DOCTYPE html><html><body>Refined by Gemini</body></html>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Refined by Gemini") || result.contains("<html>"));
        }
    }

    @Test
    @DisplayName("Happy Path: refineHtmlTemplate - Preserves table structure from original")
    void testRefineHtmlTemplate_Success_PreservesTableStructure() {
        // Arrange
        String htmlWithComplexTable = "<html><body><table border=\"1\"><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<table border=\\\"1\\\"><tr><td>Cell 1</td><td>Cell 2</td></tr></table>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(htmlWithComplexTable, TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("table") || result.contains("Cell"));
        }
    }

    @Test
    @DisplayName("Happy Path: refineHtmlTemplate - Raw response gets boilerplate wrapper")
    void testRefineHtmlTemplate_Success_RawResponseGetsBoilerplate() throws Exception {
        // Arrange
        String rawResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<p>Just plain content</p>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(rawResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.startsWith("<!DOCTYPE html>"));
        }
    }

    // ============ refineHtmlTemplate EXCEPTION PATH TESTS ============

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - Groq API key is null")
    void testRefineHtmlTemplate_Exception_GroqApiKeyNull() {
        // Arrange
        ReflectionTestUtils.setField(aiTemplateService, "groqApiKey", null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Groq API Key is missing"));
    }

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - Vault not found for Gemini")
    void testRefineHtmlTemplate_Exception_VaultNotFound() {
        // Arrange
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("API Vault not configured"));
    }

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - Decryption fails")
    void testRefineHtmlTemplate_Exception_DecryptionFails() throws Exception {
        // Arrange
        Vault mockVault = new Vault();
        mockVault.setGeminiApiKeyEncrypted("encrypted_key");
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(mockVault));
        when(encryptionUtil.decrypt("encrypted_key")).thenThrow(new RuntimeException("Decryption error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Failed to decrypt Gemini Key"));
    }

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - Groq REST call fails")
    void testRefineHtmlTemplate_Exception_GroqRestCallFails() {
        // Arrange
        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class)))
                            .thenThrow(new RestClientException("Service unavailable"));
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Groq AI Error"));
        }
    }

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - Gemini REST call fails")
    void testRefineHtmlTemplate_Exception_GeminiRestCallFails() throws Exception {
        // Arrange
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class)))
                            .thenThrow(new RestClientException("Network error"));
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Gemini AI Error"));
        }
    }

    @Test
    @DisplayName("Exception Path: refineHtmlTemplate - JSON parsing fails")
    void testRefineHtmlTemplate_Exception_JsonParsingFails() {
        // Arrange
        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn("{broken json");
                })) {

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                aiTemplateService.refineHtmlTemplate(TEST_HTML, TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL)
            );

            assertTrue(exception.getMessage().contains("Groq AI Error"));
        }
    }

    // ============ EDGE CASE TESTS ============

    @Test
    @DisplayName("Edge Case: generateHtmlTemplate - Empty topic string")
    void testGenerateHtmlTemplate_EdgeCase_EmptyTopic() {
        // Arrange
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<html></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate("", GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Edge Case: generateHtmlTemplate - Very long topic string")
    void testGenerateHtmlTemplate_EdgeCase_VeryLongTopic() {
        // Arrange
        String longTopic = "A".repeat(1000);
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<html></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(longTopic, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Edge Case: generateHtmlTemplate - Null HTML in response")
    void testGenerateHtmlTemplate_EdgeCase_NullHtmlInResponse() {
        // Arrange
        String responseWithNull = "{\"choices\":[{\"message\":{\"content\":null}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(responseWithNull);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("Edge Case: refineHtmlTemplate - Empty HTML input")
    void testRefineHtmlTemplate_EdgeCase_EmptyHtmlInput() {
        // Arrange
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<html></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate("", TEST_INSTRUCTIONS, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Edge Case: refineHtmlTemplate - Empty instructions")
    void testRefineHtmlTemplate_EdgeCase_EmptyInstructions() throws Exception {
        // Arrange
        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<html></html>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(TEST_HTML, "", GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Edge Case: generateHtmlTemplate - Groq response with extra whitespace")
    void testGenerateHtmlTemplate_EdgeCase_ResponseWithWhitespace() {
        // Arrange
        String groqResponseWithWhitespace = "{\"choices\":[{\"message\":{\"content\":\"  \\n\\n  <html></html>  \\n  \"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponseWithWhitespace);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertFalse(result.startsWith(" "));
        }
    }

    @Test
    @DisplayName("Edge Case: refineHtmlTemplate - HTML with HTML entities")
    void testRefineHtmlTemplate_EdgeCase_HtmlWithEntities() throws Exception {
        // Arrange
        String htmlWithEntities = "<html><body>&nbsp;&lt;tag&gt;&amp;</body></html>";
        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<html><body>Modified with entities</body></html>\"}]}}]}";
        setupVaultAndEncryption();

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(htmlWithEntities, TEST_INSTRUCTIONS, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Edge Case: generateHtmlTemplate - Case-insensitive provider matching")
    void testGenerateHtmlTemplate_EdgeCase_CaseInsensitiveProvider() {
        // Arrange
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<html></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act - provider is uppercase
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, "GROQ", TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    private void setupVaultAndEncryption() throws Exception {
        Vault mockVault = new Vault();
        mockVault.setGeminiApiKeyEncrypted("encrypted_gemini_key");
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(mockVault));
        when(encryptionUtil.decrypt("encrypted_gemini_key")).thenReturn(GEMINI_API_KEY);
    }

    @Test
    @DisplayName("Integration: generateHtmlTemplate - Gemini with special characters in API key")
    void testGenerateHtmlTemplate_Integration_GeminiWithSpecialCharsInKey() throws Exception {
        // Arrange
        String specialKey = "AIzaSyD@#$%^&*()_special_key_12345";
        Vault mockVault = new Vault();
        mockVault.setGeminiApiKeyEncrypted("encrypted_key");
        when(vaultRepository.findByUserEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(mockVault));
        when(encryptionUtil.decrypt("encrypted_key")).thenReturn(specialKey);

        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"<html></html>\"}]}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(geminiResponse);
                })) {

            // Act
            String result = aiTemplateService.generateHtmlTemplate(TEST_TOPIC, GEMINI_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Integration: refineHtmlTemplate - Complex multi-line instructions")
    void testRefineHtmlTemplate_Integration_ComplexInstructions() {
        // Arrange
        String complexInstructions = "Change background to gradient blue\n" +
                "Add shadow effects\n" +
                "Increase padding by 20px\n" +
                "Update font size to 16px";
        String groqResponse = "{\"choices\":[{\"message\":{\"content\":\"<html><body>Updated</body></html>\"}}]}";

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForObject(anyString(), any(), eq(String.class))).thenReturn(groqResponse);
                })) {

            // Act
            String result = aiTemplateService.refineHtmlTemplate(TEST_HTML, complexInstructions, GROQ_PROVIDER, TEST_USER_EMAIL);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Updated"));
        }
    }
}
