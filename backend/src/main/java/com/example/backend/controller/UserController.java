package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    // The frontend will hit this endpoint the moment a user logs in
    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(userService.getOrCreateUser(email));
    }
}