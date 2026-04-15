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
import java.util.stream.Collectors;

@Service
public class SubscriberService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    public int importCsv(MultipartFile file) {
        int importedCount = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String[] headers = null;

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");

                // Capture the first row as headers
                if (headers == null) {
                    headers = columns;
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = headers[i].trim().toLowerCase(); // Normalize headers
                    }
                    continue;
                }

                if (columns.length == 0) continue;

                Subscriber subscriber = new Subscriber();
                String email = null;

                // Dynamically map each column based on its header
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
                        // Anything else goes into the dynamic JSON bucket!
                        subscriber.getCustomAttributes().put(header, value);
                    }
                }

                // Skip if no email, or if email already exists
                if (email == null || email.isEmpty() || subscriberRepository.existsByEmail(email)) {
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

    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }
    // Scans all subscribers to find unique custom columns for the frontend dropdown
    public List<String> getUniqueCustomAttributes() {
        return subscriberRepository.findAll().stream()
                .filter(sub -> sub.getCustomAttributes() != null) // THE FIX: Skip legacy users with null data
                .flatMap(sub -> sub.getCustomAttributes().keySet().stream())
                .distinct()
                .collect(Collectors.toList());
    }
    public void deleteSubscriber(Long id) {
        if(subscriberRepository.existsById(id)) {
            subscriberRepository.deleteById(id);
        }
    }
}