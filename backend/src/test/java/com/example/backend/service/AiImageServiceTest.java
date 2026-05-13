package com.example.backend.service;

import com.example.backend.entity.Media;
import com.example.backend.entity.Vault;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiImageServiceTest {

    @Mock
    private MediaService mediaService;

    @Mock
    private VaultRepository vaultRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private AiImageService aiImageService;

    // ─────────────────────────────────────────────────────────────────────────
    // Pollinations Tests (Provider != "gemini")
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void generateAndSaveImage_WhenPollinationsReturnsData_ShouldSaveAndReturnMedia() throws Exception {
        byte[] fakeImageBytes = "fake-image-data".getBytes();
        Media expectedMedia = new Media();
        
        when(mediaService.saveMediaFromBytes(any(byte[].class), eq("pollinations_ai.jpg"), eq("image/jpeg"), eq("test@example.com")))
                .thenReturn(expectedMedia);

        // Mock the RestTemplate constructor to return fake data
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.getForObject(anyString(), eq(byte[].class))).thenReturn(fakeImageBytes);
        })) {
            Media result = aiImageService.generateAndSaveImage("a cool dog", "pollinations", "test@example.com");
            
            assertNotNull(result);
            verify(mediaService, times(1)).saveMediaFromBytes(any(byte[].class), anyString(), anyString(), anyString());
        }
    }

    @Test
    void generateAndSaveImage_WhenPollinationsReturnsNull_ShouldThrowException() throws Exception {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.getForObject(anyString(), eq(byte[].class))).thenReturn(null);
        })) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                aiImageService.generateAndSaveImage("a cool dog", "pollinations", "test@example.com")
            );
            
            assertTrue(ex.getMessage().contains("Free AI returned empty data"));
            verify(mediaService, never()).saveMediaFromBytes(any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void generateAndSaveImage_WhenPollinationsThrowsException_ShouldWrapAndThrow() throws Exception {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.getForObject(anyString(), eq(byte[].class))).thenThrow(new RuntimeException("Connection Refused"));
        })) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                aiImageService.generateAndSaveImage("a cool dog", "pollinations", "test@example.com")
            );
            
            assertTrue(ex.getMessage().contains("Free AI Image Error: Connection Refused"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gemini Tests (Provider == "gemini")
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void generateAndSaveImage_WhenGeminiConfigMissing_ShouldThrowException() {
        when(vaultRepository.findByUserEmail("test@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            aiImageService.generateAndSaveImage("a cool dog", "gemini", "test@example.com")
        );
        
        assertEquals("API Vault not configured.", ex.getMessage());
        verifyNoInteractions(encryptionUtil);
    }



    @Test
    void generateAndSaveImage_WhenGeminiKeyEmpty_ShouldThrowException() throws Exception {
        Vault vault = new Vault();
        vault.setGeminiApiKeyEncrypted("encrypted");
        when(vaultRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(vault));
        when(encryptionUtil.decrypt("encrypted")).thenReturn("  "); // Blank key

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            aiImageService.generateAndSaveImage("a cool dog", "gemini", "test@example.com")
        );
        
        assertEquals("API Key is empty.", ex.getMessage());
    }

    @Test
    void generateAndSaveImage_WhenGeminiReturnsValidData_ShouldSaveAndReturnMedia() throws Exception {
        Vault vault = new Vault();
        vault.setGeminiApiKeyEncrypted("encrypted");
        when(vaultRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(vault));
        when(encryptionUtil.decrypt("encrypted")).thenReturn("valid-key");

        // Mocked Google AI JSON Response
        String fakeResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"data\":\"ZmFrZS1iYXNlNjQtaW1hZ2U=\",\"mimeType\":\"image/png\"}}]}}]}";
        Media expectedMedia = new Media();
        
        when(mediaService.saveMediaFromBytes(any(byte[].class), eq("gemini_image.png"), eq("image/png"), eq("test@example.com")))
                .thenReturn(expectedMedia);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(fakeResponse);
        })) {
            Media result = aiImageService.generateAndSaveImage("a cool dog", "gemini", "test@example.com");
            
            assertNotNull(result);
            verify(mediaService, times(1)).saveMediaFromBytes(any(byte[].class), anyString(), anyString(), anyString());
        }
    }

    @Test
    void generateAndSaveImage_WhenGeminiRejected_ShouldThrowException() throws Exception {
        Vault vault = new Vault();
        vault.setGeminiApiKeyEncrypted("encrypted");
        when(vaultRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(vault));
        when(encryptionUtil.decrypt("encrypted")).thenReturn("valid-key");

        String fakeResponse = "{\"error\": \"No candidates available\"}"; 

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(fakeResponse);
        })) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                aiImageService.generateAndSaveImage("a cool dog", "gemini", "test@example.com")
            );
            
            assertTrue(ex.getMessage().contains("Google rejected the request"));
            verify(mediaService, never()).saveMediaFromBytes(any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void generateAndSaveImage_WhenGeminiQuotaExceeded_ShouldThrowException() throws Exception {
        Vault vault = new Vault();
        vault.setGeminiApiKeyEncrypted("encrypted");
        when(vaultRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(vault));
        when(encryptionUtil.decrypt("encrypted")).thenReturn("valid-key");

        HttpClientErrorException exception429 = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenThrow(exception429);
        })) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                aiImageService.generateAndSaveImage("a cool dog", "gemini", "test@example.com")
            );
            
            assertTrue(ex.getMessage().contains("API Quota Exceeded"));
        }
    }
}
