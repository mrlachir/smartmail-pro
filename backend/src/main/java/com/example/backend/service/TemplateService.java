package com.example.backend.service;

import com.example.backend.entity.Template;
import com.example.backend.entity.User;
import com.example.backend.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserService userService;

    public List<Template> getAllTemplates(String userEmail) {
        return templateRepository.findByUserEmail(userEmail);
    }

    public Template saveTemplate(Template template, String userEmail) {
        User user = userService.getOrCreateUser(userEmail);

        // Prevent duplicate template names for the same user
        if (template.getId() == null && templateRepository.existsByNameAndUserEmail(template.getName(), userEmail)) {
            throw new RuntimeException("A template with this name already exists.");
        }

        template.setUser(user);
        return templateRepository.save(template);
    }

    public Template updateTemplate(Long id, Template updatedData, String userEmail) {
        Template existing = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // SECURITY: Verify the user owns this template
        if (!existing.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized to edit this template.");
        }

        existing.setName(updatedData.getName());
        existing.setHtmlContent(updatedData.getHtmlContent());
        return templateRepository.save(existing);
    }

    public void deleteTemplate(Long id, String userEmail) {
        templateRepository.findById(id).ifPresent(template -> {
            // SECURITY: Verify ownership before deleting
            if (template.getUser().getEmail().equals(userEmail)) {
                templateRepository.deleteById(id);
            }
        });
    }
}