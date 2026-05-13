package com.example.backend.controller;

import com.example.backend.entity.Campaign;
import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.entity.Template;
import com.example.backend.repository.CampaignRepository;
import com.example.backend.repository.EmailInteractionRepository;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.TemplateRepository;
import com.example.backend.service.EnterpriseEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private EmailInteractionRepository interactionRepository;

    @Mock
    private EnterpriseEmailService emailService;

    @InjectMocks
    private CampaignController campaignController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(campaignController).build();
    }

    @Test
    @DisplayName("GET /api/campaigns returns campaigns for user")
    void getCampaigns_ReturnsCampaignList() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Spring Launch");
        campaign.setUserEmail("user@example.com");

        when(campaignRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(campaign));

        mockMvc.perform(get("/api/campaigns")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spring Launch")));
    }

    @Test
    @DisplayName("POST /api/campaigns/launch schedules campaign when future date is provided")
    void launchCampaign_SchedulesCampaign_WhenFutureDatePresent() throws Exception {
        Segment segment = new Segment();
        Subscriber subscriber = new Subscriber();
        subscriber.setId(10L);
        subscriber.setEmail("recipient@example.com");
        segment.setSubscribers(Set.of(subscriber));

        Template template = new Template();
        template.setHtmlContent("<html>campaign</html>");

        when(segmentRepository.findById(5L)).thenReturn(Optional.of(segment));
        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String scheduledAt = LocalDateTime.now().plusDays(1).toString();
        Map<String, Object> payload = Map.of(
                "segmentId", 5,
                "templateId", 7,
                "name", "Scheduled Campaign",
                "subject", "Hello",
                "scheduledAt", scheduledAt
        );

        mockMvc.perform(post("/api/campaigns/launch")
                        .header("X-User-Email", "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Campaign scheduled to launch at")));
    }

    @Test
    @DisplayName("POST /api/campaigns/launch launches immediately when no scheduled date is provided")
    void launchCampaign_LaunchesImmediately_WhenNoScheduledDate() throws Exception {
        Segment segment = new Segment();
        Subscriber subscriber = new Subscriber();
        subscriber.setId(10L);
        subscriber.setEmail("recipient@example.com");
        segment.setSubscribers(Set.of(subscriber));

        Template template = new Template();
        template.setHtmlContent("<html>campaign</html>");

        when(segmentRepository.findById(5L)).thenReturn(Optional.of(segment));
        when(templateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = Map.of(
                "segmentId", 5,
                "templateId", 7,
                "name", "Immediate Campaign",
                "subject", "Hello"
        );

        mockMvc.perform(post("/api/campaigns/launch")
                        .header("X-User-Email", "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sent to 1 recipients.")));
    }

    @Test
    @DisplayName("POST /api/campaigns/launch returns bad request when segment is missing")
    void launchCampaign_ReturnsBadRequest_WhenSegmentNotFound() throws Exception {
        when(segmentRepository.findById(5L)).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
                "segmentId", 5,
                "templateId", 7,
                "name", "Missing Segment",
                "subject", "Hello"
        );

        mockMvc.perform(post("/api/campaigns/launch")
                        .header("X-User-Email", "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Segment not found")));
    }

    @Test
    @DisplayName("GET /api/campaigns/{id}/stats returns stats when authorized")
    void getCampaignStats_ReturnsStats_WhenAuthorized() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setUserEmail("user@example.com");
        Segment segment = new Segment();
        Subscriber subscriber1 = new Subscriber();
        subscriber1.setId(101L);
        subscriber1.setEmail("subscriber1@example.com");
        Subscriber subscriber2 = new Subscriber();
        subscriber2.setId(102L);
        subscriber2.setEmail("subscriber2@example.com");
        segment.setSubscribers(Set.of(subscriber1, subscriber2));
        campaign.setSegment(segment);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(interactionRepository.countByCampaignIdAndInteractionType(1L, "OPEN")).thenReturn(5L);
        when(interactionRepository.countByCampaignIdAndInteractionType(1L, "CLICK")).thenReturn(2L);

        mockMvc.perform(get("/api/campaigns/1/stats")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalSent\":2")))
                .andExpect(content().string(containsString("\"uniqueOpens\":5")))
                .andExpect(content().string(containsString("\"uniqueClicks\":2")));
    }

    @Test
    @DisplayName("GET /api/campaigns/{id}/stats returns forbidden when unauthorized")
    void getCampaignStats_ReturnsForbidden_WhenUnauthorized() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setUserEmail("other@example.com");

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        mockMvc.perform(get("/api/campaigns/1/stats")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Unauthorized")));
    }

    @Test
    @DisplayName("DELETE /api/campaigns/{id} deletes campaign when authorized")
    void deleteCampaign_DeletesCampaign_WhenAuthorized() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setUserEmail("user@example.com");

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        doNothing().when(interactionRepository).deleteByCampaignId(1L);

        mockMvc.perform(delete("/api/campaigns/1")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Campaign deleted successfully")));

        verify(interactionRepository).deleteByCampaignId(1L);
    }

    @Test
    @DisplayName("DELETE /api/campaigns/{id} returns bad request when campaign not found")
    void deleteCampaign_ReturnsBadRequest_WhenNotFound() throws Exception {
        when(campaignRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/campaigns/1")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Campaign not found in database.")));
    }
}
