package com.example.backend.repository;

import com.example.backend.model.FeatureSnapshot;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureSnapshotRepository extends JpaRepository<FeatureSnapshot, Long> {
    Optional<FeatureSnapshot> findByUserAndMonth(User user, String month);
}
