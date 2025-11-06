package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "security_questions")
@Getter @Setter
public class SecurityQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String questionText;

    // @CreationTimestamp
    // @Column(nullable = false, updatable = false)
    // private LocalDateTime created_at = LocalDateTime.now();
}
