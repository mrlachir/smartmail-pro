package com.example.backend.service;

import com.example.backend.entity.Media;
import com.example.backend.entity.User;
import com.example.backend.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private UserService userService;

    // The physical folder on your machine
    private final String UPLOAD_DIR = "uploads/";

    public MediaService() {
        // Create the folder immediately when the server starts if it doesn't exist
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public Media saveMedia(MultipartFile file, String userEmail) throws IOException {
        User user = userService.getOrCreateUser(userEmail);

        // Sanitize the filename to prevent path injection and avoid spacing errors
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png";
        String cleanName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String uniqueFileName = System.currentTimeMillis() + "_" + cleanName;

        // Save the physical file to the hard drive
        Path filePath = Paths.get(UPLOAD_DIR + uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save the metadata to the database
        Media media = new Media();
        media.setFileName(uniqueFileName);
        // The URL Next.js will use to render the image
        media.setFileUrl("http://localhost:8080/uploads/" + uniqueFileName);
        media.setFileType(file.getContentType());
        media.setUser(user);

        return mediaRepository.save(media);
    }
    // THE UPGRADE: Save raw bytes directly from the AI Generator
    public Media saveMediaFromBytes(byte[] imageBytes, String originalName, String mimeType, String userEmail) throws IOException {
        User user = userService.getOrCreateUser(userEmail);

        String cleanName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String uniqueFileName = System.currentTimeMillis() + "_" + cleanName;

        Path filePath = Paths.get(UPLOAD_DIR + uniqueFileName);
        Files.write(filePath, imageBytes);

        Media media = new Media();
        media.setFileName(uniqueFileName);
        media.setFileUrl("http://localhost:8080/uploads/" + uniqueFileName);
        media.setFileType(mimeType);
        media.setUser(user);

        return mediaRepository.save(media);
    }

    public List<Media> getUserGallery(String userEmail) {
        return mediaRepository.findByUserEmail(userEmail);
    }
}