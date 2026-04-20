package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.entity.Vault;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VaultService {

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private UserService userService;

    // THE FIX: Inject the encryption engine
    @Autowired
    private EncryptionUtil encryptionUtil;

    public Vault getVaultByUserEmail(String email) {
        return vaultRepository.findByUserEmail(email).orElse(null);
    }

    public Vault saveVault(Vault vault, String email) {
        User user = userService.getOrCreateUser(email);
        Vault existingVault = vaultRepository.findByUserEmail(email).orElse(new Vault());

        existingVault.setUser(user);

        try {
            // THE FIX: Intercept the raw text and encrypt it BEFORE saving to the database
            if (vault.getGeminiApiKeyEncrypted() != null && !vault.getGeminiApiKeyEncrypted().isEmpty()) {
                String encryptedKey = encryptionUtil.encrypt(vault.getGeminiApiKeyEncrypted());
                existingVault.setGeminiApiKeyEncrypted(encryptedKey);
            }

            if (vault.getGmailOauthTokenEncrypted() != null && !vault.getGmailOauthTokenEncrypted().isEmpty()) {
                String encryptedToken = encryptionUtil.encrypt(vault.getGmailOauthTokenEncrypted());
                existingVault.setGmailOauthTokenEncrypted(encryptedToken);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt sensitive data before saving.");
        }

        return vaultRepository.save(existingVault);
    }
}