package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_recommendation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","month"})
)
@Data
@Setter
@Getter
public class AiRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rec_id", nullable = false, updatable = false)
    private Long recId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "month", nullable = false, length = 7) // YYYY-MM
    private String month;

    @Column(name = "language", nullable = false, length = 16) // zh-CN / en-AU 等
    private String language;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}