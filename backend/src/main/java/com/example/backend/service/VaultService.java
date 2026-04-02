package com.example.backend.service;

import com.example.backend.entity.Vault;
import com.example.backend.repository.VaultRepository;
import com.example.backend.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service // Tells Spring Boot this class handles business logic
public class VaultService {

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    // This method is called when the user submits the form
    public void saveKeys(Long userId, String geminiKey, String gmailToken) {
        // 1. Check if this user already has a vault
        Vault vault = vaultRepository.findByUserId(userId);

        // 2. If not, create a new one
        if (vault == null) {
            vault = new Vault();
            vault.setUserId(userId);
        }

        // 3. Encrypt the keys and attach them to the vault
        if (geminiKey != null && !geminiKey.isEmpty()) {
            vault.setGeminiApiKeyEncrypted(encryptionUtil.encrypt(geminiKey));
        }
        if (gmailToken != null && !gmailToken.isEmpty()) {
            vault.setGmailOauthTokenEncrypted(encryptionUtil.encrypt(gmailToken));
        }

        // 4. Save to MySQL
        vaultRepository.save(vault);
    }

    // This method checks if keys exist without exposing the actual keys
    public boolean hasKeys(Long userId) {
        Vault vault = vaultRepository.findByUserId(userId);
        return vault != null && vault.getGeminiApiKeyEncrypted() != null;
    }
}