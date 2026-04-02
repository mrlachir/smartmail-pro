package com.example.backend.service;

import com.example.backend.entity.Subscriber;
import com.example.backend.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SubscriberService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    // This method expects a CSV file where columns are: Email, FirstName, LastName
    public int importCsv(MultipartFile file) {
        int importedCount = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true; // Skip the header row (e.g., "email,first_name,last_name")

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length < 1) continue; // Skip empty lines

                String email = columns[0].trim();

                // If the email is blank or already exists, skip it to prevent crashes
                if (email.isEmpty() || subscriberRepository.existsByEmail(email)) {
                    continue;
                }

                Subscriber subscriber = new Subscriber();
                subscriber.setEmail(email);

                if (columns.length > 1) {
                    subscriber.setFirstName(columns[1].trim());
                }
                if (columns.length > 2) {
                    subscriber.setLastName(columns[2].trim());
                }

                subscriberRepository.save(subscriber);
                importedCount++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        return importedCount;
    }

    // Fetch all subscribers for the frontend table
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }

    // Delete a subscriber by ID
    public void deleteSubscriber(Long id) {
        if(subscriberRepository.existsById(id)) {
            subscriberRepository.deleteById(id);
        }
    }
}