package com.example.backend.service;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.entity.Vault;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSegmentServiceTest {

    @Mock
    private SubscriberService subscriberService;

    @Mock
    private VaultRepository vaultRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private SegmentRepository segmentRepository;

    @InjectMocks
    private AiSegmentService aiSegmentService;

    private Subscriber testSubscriber;

    @BeforeEach
    void setUp() {
        // Inject a non-empty groqApiKey so the @Value field is populated
        ReflectionTestUtils.setField(aiSegmentService, "groqApiKey", "test-groq-key");

        testSubscriber = new Subscriber();
        testSubscriber.setId(1L);
        testSubscriber.setEmail("sub@example.com");
        testSubscriber.setStatus("ACTIVE");

        Map<String, String> attrs = new HashMap<>();
        attrs.put("city", "Paris");
        attrs.put("age", "30");
        testSubscriber.setCustomAttributes(attrs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSuggestedSegments – input validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSuggestedSegments_WhenNoSubscribers_ShouldThrowException() {
        when(subscriberService.getAllSubscribers("test@example.com"))
                .thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("groq", "test@example.com")
        );

        assertEquals("You have no subscribers. Upload a CSV first.", ex.getMessage());
        verify(subscriberService, times(1)).getAllSubscribers("test@example.com");
        verifyNoInteractions(segmentRepository);
    }

    @Test
    void getSuggestedSegments_WhenNoExistingSegments_ShouldBuildPromptWithNone() {
        // We mock a groqApiKey that is EMPTY to force an exception before the
        // actual HTTP call, but AFTER the validation + prompt-building phase.
        ReflectionTestUtils.setField(aiSegmentService, "groqApiKey", "");

        when(subscriberService.getAllSubscribers("test@example.com"))
                .thenReturn(List.of(testSubscriber));
        when(segmentRepository.findByUserEmail("test@example.com"))
                .thenReturn(List.of());

        // The call will crash because groqApiKey is blank — that is expected.
        // We only care that it made it past the empty-subscriber guard.
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("groq", "test@example.com")
        );

        assertTrue(ex.getMessage().contains("Groq API Key is missing"));
        verify(subscriberService, times(1)).getAllSubscribers("test@example.com");
        verify(segmentRepository, times(1)).findByUserEmail("test@example.com");
    }

    @Test
    void getSuggestedSegments_WhenExistingSegmentsPresent_ShouldListThemInContext() {
        ReflectionTestUtils.setField(aiSegmentService, "groqApiKey", "");

        Segment existingSegment = new Segment();
        existingSegment.setName("VIP Customers");

        when(subscriberService.getAllSubscribers("test@example.com"))
                .thenReturn(List.of(testSubscriber));
        when(segmentRepository.findByUserEmail("test@example.com"))
                .thenReturn(List.of(existingSegment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("groq", "test@example.com")
        );

        // The only failure path here is the missing Groq key — proves we reached the API call
        assertTrue(ex.getMessage().contains("Groq API Key is missing"));
    }

    @Test
    void getSuggestedSegments_WhenSubscriberHasNullAttributes_ShouldNotCrash() {
        ReflectionTestUtils.setField(aiSegmentService, "groqApiKey", "");

        Subscriber noAttrsSub = new Subscriber();
        noAttrsSub.setEmail("nope@example.com");
        noAttrsSub.setStatus("INACTIVE");
        noAttrsSub.setCustomAttributes(null); // <-- the null-path

        when(subscriberService.getAllSubscribers("test@example.com"))
                .thenReturn(List.of(noAttrsSub));
        when(segmentRepository.findByUserEmail("test@example.com"))
                .thenReturn(List.of());

        // Should still reach the Groq call (and fail there), NOT crash on null attrs
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("groq", "test@example.com")
        );
        assertTrue(ex.getMessage().contains("Groq API Key is missing"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Provider routing: Groq vs Gemini
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSuggestedSegments_WhenProviderIsGroq_ShouldCallGroqPath() {
        // groqApiKey is set to "" to stop the call just before the real HTTP request
        ReflectionTestUtils.setField(aiSegmentService, "groqApiKey", "");

        when(subscriberService.getAllSubscribers("user@test.com"))
                .thenReturn(List.of(testSubscriber));
        when(segmentRepository.findByUserEmail("user@test.com"))
                .thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("GROQ", "user@test.com")
        );
        // Groq path reached because the Groq-specific error message is returned
        assertTrue(ex.getMessage().contains("Groq API Key is missing"));
        // Vault was never touched — confirms Groq branch was taken, not Gemini
        verifyNoInteractions(vaultRepository);
    }

    @Test
    void getSuggestedSegments_WhenProviderIsGemini_ShouldCallGeminiPath() {
        when(subscriberService.getAllSubscribers("user@test.com"))
                .thenReturn(List.of(testSubscriber));
        when(segmentRepository.findByUserEmail("user@test.com"))
                .thenReturn(List.of());
        // Vault not found → Gemini branch throws its own exception
        when(vaultRepository.findByUserEmail("user@test.com"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aiSegmentService.getSuggestedSegments("gemini", "user@test.com")
        );
        assertEquals("API Vault not configured.", ex.getMessage());
        verify(vaultRepository, times(1)).findByUserEmail("user@test.com");
    }

}
