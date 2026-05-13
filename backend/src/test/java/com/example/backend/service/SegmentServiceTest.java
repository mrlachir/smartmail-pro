package com.example.backend.service;

import com.example.backend.entity.Segment;
import com.example.backend.entity.Subscriber;
import com.example.backend.entity.User;
import com.example.backend.repository.SegmentRepository;
import com.example.backend.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegmentServiceTest {

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SegmentService segmentService;

    private User testUser;
    private Segment testSegment;
    private Subscriber testSubscriber;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");

        testSegment = new Segment();
        testSegment.setId(1L);
        testSegment.setName("Test Segment");
        testSegment.setUser(testUser);
        testSegment.setRules("[{\"column\":\"status\",\"operator\":\"=\",\"value\":\"ACTIVE\"}]");

        testSubscriber = new Subscriber();
        testSubscriber.setId(10L);
        testSubscriber.setEmail("sub@example.com");
        testSubscriber.setStatus("ACTIVE");
        testSubscriber.setUser(testUser);
        testSubscriber.setCustomAttributes(java.util.Map.of("dummy", "value"));
    }

    @Test
    void saveSegment_ShouldSaveAndReturnSegment() {
        when(userService.getOrCreateUser(anyString())).thenReturn(testUser);
        when(subscriberRepository.findByUserEmail(anyString())).thenReturn(List.of(testSubscriber));
        when(segmentRepository.save(any(Segment.class))).thenReturn(testSegment);

        Segment savedSegment = segmentService.saveSegment(testSegment, "test@example.com");

        assertNotNull(savedSegment);
        assertEquals("Test Segment", savedSegment.getName());
        verify(userService, times(1)).getOrCreateUser("test@example.com");
        verify(subscriberRepository, times(1)).findByUserEmail("test@example.com");
        verify(segmentRepository, times(1)).save(testSegment);
    }

    @Test
    void applyRulesAndLinkSubscribers_ShouldLinkMatchingSubscribers() {
        when(subscriberRepository.findByUserEmail("test@example.com")).thenReturn(List.of(testSubscriber));

        segmentService.applyRulesAndLinkSubscribers(testSegment, "test@example.com");

        assertNotNull(testSegment.getSubscribers());
        assertEquals(1, testSegment.getSubscribers().size());
        assertTrue(testSegment.getSubscribers().contains(testSubscriber));
        verify(subscriberRepository, times(1)).findByUserEmail("test@example.com");
    }

    @Test
    void getAllSegments_ShouldReturnListOfSegments() {
        when(segmentRepository.findByUserEmail("test@example.com")).thenReturn(List.of(testSegment));

        List<Segment> segments = segmentService.getAllSegments("test@example.com");

        assertNotNull(segments);
        assertEquals(1, segments.size());
        verify(segmentRepository, times(1)).findByUserEmail("test@example.com");
    }

    @Test
    void updateSegment_WhenAuthorized_ShouldUpdateAndReturnSegment() {
        Segment updatedData = new Segment();
        updatedData.setName("Updated Segment");
        updatedData.setDescription("Updated Desc");
        updatedData.setRules("[]");

        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));
        when(segmentRepository.save(any(Segment.class))).thenReturn(testSegment);

        Segment result = segmentService.updateSegment(1L, updatedData, "test@example.com");

        assertNotNull(result);
        assertEquals("Updated Segment", testSegment.getName());
        assertEquals("Updated Desc", testSegment.getDescription());
        verify(segmentRepository, times(1)).findById(1L);
        verify(segmentRepository, times(1)).save(testSegment);
    }

    @Test
    void updateSegment_WhenUnauthorized_ShouldThrowException() {
        Segment updatedData = new Segment();
        
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            segmentService.updateSegment(1L, updatedData, "wrong@example.com");
        });

        assertEquals("Unauthorized", exception.getMessage());
        verify(segmentRepository, times(1)).findById(1L);
        verify(segmentRepository, never()).save(any(Segment.class));
    }

    @Test
    void updateSegment_WhenNotFound_ShouldThrowException() {
        when(segmentRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            segmentService.updateSegment(1L, new Segment(), "test@example.com");
        });

        assertEquals("Segment not found", exception.getMessage());
        verify(segmentRepository, times(1)).findById(1L);
    }

    @Test
    void deleteSegment_WhenAuthorized_ShouldDeleteSegment() {
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));

        segmentService.deleteSegment(1L, "test@example.com");

        verify(segmentRepository, times(1)).findById(1L);
        verify(segmentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteSegment_WhenUnauthorized_ShouldNotDeleteSegment() {
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));

        segmentService.deleteSegment(1L, "wrong@example.com");

        verify(segmentRepository, times(1)).findById(1L);
        verify(segmentRepository, never()).deleteById(anyLong());
    }

    @Test
    void getSubscribersInSegment_WhenAuthorized_ShouldReturnMatchingSubscribers() {
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));
        when(subscriberRepository.findByUserEmail("test@example.com")).thenReturn(List.of(testSubscriber));

        List<Subscriber> result = segmentService.getSubscribersInSegment(1L, "test@example.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sub@example.com", result.get(0).getEmail());
        verify(segmentRepository, times(1)).findById(1L);
        verify(subscriberRepository, times(1)).findByUserEmail("test@example.com");
    }

    @Test
    void getSubscribersInSegment_WhenUnauthorized_ShouldThrowException() {
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            segmentService.getSubscribersInSegment(1L, "wrong@example.com");
        });

        assertEquals("Unauthorized access to segment", exception.getMessage());
        verify(segmentRepository, times(1)).findById(1L);
        verify(subscriberRepository, never()).findByUserEmail(anyString());
    }
}
