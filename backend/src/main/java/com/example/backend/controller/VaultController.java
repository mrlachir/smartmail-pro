package com.example.backend.controller;

import com.example.backend.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vault") // The base URL for this controller
@CrossOrigin(origins = "http://localhost:3000") // Allows your Next.js frontend to call this API
public class VaultController {

    @Autowired
    private VaultService vaultService;

    @PostMapping("/update")
    public ResponseEntity<?> updateVault(@RequestBody Map<String, String> payload) {
        // NOTE: In a fully finished app, we extract the userId from the Google SSO token.
        // For right now, we will simulate user ID 1 (The Admin).
        Long adminId = 1L;

        String geminiKey = payload.get("geminiKey");
        String gmailToken = payload.get("gmailToken");

        // Pass the raw keys to the service to be encrypted and saved
        vaultService.saveKeys(adminId, geminiKey, gmailToken);

        return ResponseEntity.ok().body(Map.of("message", "Keys encrypted and saved securely."));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getVaultStatus() {
        Long adminId = 1L;
        boolean exists = vaultService.hasKeys(adminId);
        return ResponseEntity.ok().body(Map.of("hasKeys", exists));
    }
}