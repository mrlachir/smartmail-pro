package com.example.backend.service;

import com.example.backend.entity.Subscriber;
import com.example.backend.entity.User;
import com.example.backend.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriberService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private UserService userService; // Injects our new User logic

    public int importCsv(MultipartFile file, String userEmail) {
        // Find the user in the database, or create them if it's their first time
        User currentUser = userService.getOrCreateUser(userEmail);
        int importedCount = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String[] headers = null;

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");

                if (headers == null) {
                    headers = columns;
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = headers[i].trim().toLowerCase();
                    }
                    continue;
                }

                if (columns.length == 0) continue;

                Subscriber subscriber = new Subscriber();
                subscriber.setUser(currentUser); // Lock this subscriber to the logged-in user
                String email = null;

                for (int i = 0; i < columns.length; i++) {
                    if (i >= headers.length) break;
                    String header = headers[i];
                    String value = columns[i].trim();

                    if (header.equals("email")) {
                        email = value;
                    } else if (header.equals("first_name") || header.equals("firstname")) {
                        subscriber.setFirstName(value);
                    } else if (header.equals("last_name") || header.equals("lastname")) {
                        subscriber.setLastName(value);
                    } else if (header.equals("status")) {
                        subscriber.setStatus(value);
                    } else {
                        subscriber.getCustomAttributes().put(header, value);
                    }
                }

                // THE FIX: Check if THIS user already has this email
                if (email == null || email.isEmpty() || subscriberRepository.existsByEmailAndUserEmail(email, userEmail)) {
                    continue;
                }

                subscriber.setEmail(email);
                subscriberRepository.save(subscriber);
                importedCount++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        return importedCount;
    }

    public List<Subscriber> getAllSubscribers(String userEmail) {
        // Only return subscribers belonging to this user
        return subscriberRepository.findByUserEmail(userEmail);
    }

    public void deleteSubscriber(Long id, String userEmail) {
        // Verify ownership before deleting
        subscriberRepository.findById(id).ifPresent(sub -> {
            if (sub.getUser().getEmail().equals(userEmail)) {
                subscriberRepository.deleteById(id);
            }
        });
    }

    public List<String> getUniqueCustomAttributes(String userEmail) {
        // Only scan this specific user's attributes
        return subscriberRepository.findByUserEmail(userEmail).stream()
                .filter(sub -> sub.getCustomAttributes() != null)
                .flatMap(sub -> sub.getCustomAttributes().keySet().stream())
                .distinct()
                .collect(Collectors.toList());
    }
}