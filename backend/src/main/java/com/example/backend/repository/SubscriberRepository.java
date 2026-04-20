package com.example.backend.repository;

import com.example.backend.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    List<Subscriber> findByUserEmail(String email);
    boolean existsByEmailAndUserEmail(String subscriberEmail, String userEmail);
}