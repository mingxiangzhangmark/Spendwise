package com.example.backend.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_security_answers")
@Getter @Setter
public class UserSecurityAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private SecurityQuestion question;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String answer_hash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created_at = LocalDateTime.now();
}

