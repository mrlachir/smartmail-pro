package com.example.backend.controller;

import com.example.backend.entity.Subscriber;
import com.example.backend.entity.User;
import com.example.backend.repository.EmailInteractionRepository;
import com.example.backend.repository.SubscriberRepository;
import com.example.backend.service.SubscriberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubscriberControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SubscriberService subscriberService;

    @Mock
    private EmailInteractionRepository interactionRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private SubscriberController subscriberController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriberController).build();
    }

    @Test
    @DisplayName("POST /api/subscribers/import returns success message")
    void importCsv_ReturnsSuccessMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "subscribers.csv",
                "text/csv",
                "email,first_name,last_name\nuser1@example.com,John,Doe".getBytes()
        );

        when(subscriberService.importCsv(any(MultipartFile.class), anyString())).thenReturn(1);

        mockMvc.perform(multipart("/api/subscribers/import")
                        .file(file)
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Successfully imported 1 subscribers.")));
    }

    @Test
    @DisplayName("POST /api/subscribers/import returns bad request on import failure")
    void importCsv_ReturnsBadRequest_WhenServiceThrows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "subscribers.csv",
                "text/csv",
                "email\nuser@example.com".getBytes()
        );

        when(subscriberService.importCsv(any(MultipartFile.class), anyString()))
                .thenThrow(new RuntimeException("Import failed"));

        mockMvc.perform(multipart("/api/subscribers/import")
                        .file(file)
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Import failed")));
    }

    @Test
    @DisplayName("GET /api/subscribers returns subscriber list")
    void getAllSubscribers_ReturnsSubscriberList() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);
        subscriber.setEmail("user@example.com");

        when(subscriberService.getAllSubscribers(anyString())).thenReturn(List.of(subscriber));

        mockMvc.perform(get("/api/subscribers")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user@example.com")));
    }

    @Test
    @DisplayName("DELETE /api/subscribers/{id} returns success message")
    void deleteSubscriber_ReturnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/subscribers/5")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Subscriber deleted successfully.")));
    }

    @Test
    @DisplayName("DELETE /api/subscribers/{id} returns internal server error when deletion fails")
    void deleteSubscriber_ReturnsInternalServerError_WhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
                .when(subscriberService).deleteSubscriber(5L, "user@example.com");

        mockMvc.perform(delete("/api/subscribers/5")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error deleting subscriber.")));
    }

    @Test
    @DisplayName("GET /api/subscribers/attributes returns unique custom attributes")
    void getAvailableAttributes_ReturnsAttributes() throws Exception {
        when(subscriberService.getUniqueCustomAttributes(anyString()))
                .thenReturn(List.of("age", "company"));

        mockMvc.perform(get("/api/subscribers/attributes")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("age")))
                .andExpect(content().string(containsString("company")));
    }

    @Test
    @DisplayName("GET /api/subscribers/top-engaged returns top engaged subscribers")
    void getTopEngagedSubscribers_ReturnsFilteredEngagedSubscribers() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);
        subscriber.setEmail("user@example.com");
        User user = new User();
        user.setEmail("user@example.com");
        subscriber.setUser(user);

        when(interactionRepository.findTopSubscriberIdsWithCount(any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 7L}));
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));

        mockMvc.perform(get("/api/subscribers/top-engaged")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user@example.com")))
                .andExpect(content().string(containsString("7")));
    }

    @Test
    @DisplayName("GET /api/subscribers/top-engaged returns bad request on repository failure")
    void getTopEngagedSubscribers_ReturnsBadRequest_WhenRepositoryFails() throws Exception {
        when(interactionRepository.findTopSubscriberIdsWithCount(any(PageRequest.class)))
                .thenThrow(new RuntimeException("DB failure"));

        mockMvc.perform(get("/api/subscribers/top-engaged")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("DB failure")));
    }
}
