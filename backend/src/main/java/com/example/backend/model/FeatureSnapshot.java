package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "feature_snapshot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","month"}))
@Data
@Setter
@Getter
public class FeatureSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id", nullable = false, updatable = false)
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "month", nullable = false, length = 7) // YYYY-MM
    private String month;

    @Column(name = "totals_by_category", columnDefinition = "TEXT", nullable = false)
    private String totalsByCategoryJson; // [{catId,catName,amount,pct}]

    @Column(name = "total_spending", nullable = false)
    private BigDecimal totalSpending;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

