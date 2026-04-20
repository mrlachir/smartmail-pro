package com.example.backend.controller;

import com.example.backend.entity.Vault;
import com.example.backend.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vault")
@CrossOrigin(origins = "http://localhost:3000")
public class VaultController {

    @Autowired
    private VaultService vaultService;

    @GetMapping
    public ResponseEntity<Vault> getVault(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(vaultService.getVaultByUserEmail(userEmail));
    }

    @PostMapping
    public ResponseEntity<Vault> saveVault(@RequestBody Vault vault, @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(vaultService.saveVault(vault, userEmail));
    }
}