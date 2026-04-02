package com.example.backend.repository;

import com.example.backend.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    // Allows us to quickly check if an email already exists during the CSV import
    boolean existsByEmail(String email);
}