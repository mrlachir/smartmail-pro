package com.example.backend.controller;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.service.SegmentService;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SegmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SegmentService segmentService;

    @InjectMocks
    private SegmentController segmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(segmentController).build();
    }

    @Test
    @DisplayName("POST /api/segments creates a new segment")
    void createSegment_ReturnsCreatedSegment() throws Exception {
        Segment returnedSegment = new Segment();
        returnedSegment.setId(1L);
        returnedSegment.setName("Test Segment");

        when(segmentService.saveSegment(any(Segment.class), anyString())).thenReturn(returnedSegment);

        mockMvc.perform(post("/api/segments")
                        .header("X-User-Email", "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Segment\",\"rules\":\"[]\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Segment")))
                .andExpect(content().string(containsString("1")));
    }

    @Test
    @DisplayName("GET /api/segments returns all segments")
    void getAllSegments_ReturnsSegmentList() throws Exception {
        Segment segment = new Segment();
        segment.setId(2L);
        segment.setName("All Segment");

        when(segmentService.getAllSegments(anyString())).thenReturn(List.of(segment));

        mockMvc.perform(get("/api/segments")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All Segment")));
    }

    @Test
    @DisplayName("PUT /api/segments/{id} updates the segment")
    void updateSegment_ReturnsUpdatedSegment() throws Exception {
        Segment updatedSegment = new Segment();
        updatedSegment.setId(3L);
        updatedSegment.setName("Updated Segment");

        when(segmentService.updateSegment(anyLong(), any(Segment.class), anyString())).thenReturn(updatedSegment);

        mockMvc.perform(put("/api/segments/3")
                        .header("X-User-Email", "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Segment\",\"rules\":\"[]\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Updated Segment")))
                .andExpect(content().string(containsString("3")));
    }

    @Test
    @DisplayName("DELETE /api/segments/{id} deletes the segment")
    void deleteSegment_ReturnsConfirmationMessage() throws Exception {
        mockMvc.perform(delete("/api/segments/5")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Segment deleted")));

        verify(segmentService).deleteSegment(5L, "user@example.com");
    }

    @Test
    @DisplayName("GET /api/segments/{id}/subscribers returns segment subscribers")
    void evaluateSegment_ReturnsSubscribersInSegment() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(7L);
        subscriber.setEmail("subscriber@example.com");

        when(segmentService.getSubscribersInSegment(anyLong(), anyString())).thenReturn(List.of(subscriber));

        mockMvc.perform(get("/api/segments/4/subscribers")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("subscriber@example.com")));
    }
}
