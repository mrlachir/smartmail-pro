package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "segments")
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // THE UPGRADE: Maps this segment strictly to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // We store the filtering rules as a JSON string so Next.js can read/write them easily
    @Column(columnDefinition = "TEXT", nullable = false)
    private String rules;
    // THE UPGRADE: Track if the segment was built by AI or manually
    @Column(name = "by_ai")
    private String byAi = "manual";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "segment_subscribers",
            joinColumns = @JoinColumn(name = "segment_id"),
            inverseJoinColumns = @JoinColumn(name = "subscriber_id")
    )
    private Set<Subscriber> subscribers = new HashSet<>();

}