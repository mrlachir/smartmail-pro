package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity // Tells Spring Boot: "Create a MySQL table for this class"
@Data // Lombok shortcut: Automatically generates Getters and Setters behind the scenes
@Table(name = "vault")
public class Vault {

    @Id // This is the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments (1, 2, 3...)
    private Long id;

    // We link these keys to a specific admin user
    @Column(name = "user_id", unique = true)
    private Long userId;

    // Length is set to 1000 because encrypted strings can be very long
    @Column(name = "gemini_api_key_encrypted", length = 1000)
    private String geminiApiKeyEncrypted;

    @Column(name = "gmail_oauth_token_encrypted", length = 1000)
    private String gmailOauthTokenEncrypted;
}