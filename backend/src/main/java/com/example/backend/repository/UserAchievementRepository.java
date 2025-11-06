package com.example.backend.repository;

import com.example.backend.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Integer> {

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.user_id = :userId")
    List<UserAchievement> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.user_id = :userId AND ua.achievement.code = :code")
    Optional<UserAchievement> findByUserIdAndAchievementCode(@Param("userId") Integer userId,
                                                             @Param("code") String code);
}
