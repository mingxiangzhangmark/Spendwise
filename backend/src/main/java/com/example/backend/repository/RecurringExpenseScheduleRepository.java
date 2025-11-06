package com.example.backend.repository;

import com.example.backend.model.RecurringExpenseSchedule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurringExpenseScheduleRepository extends JpaRepository<RecurringExpenseSchedule, Integer> {
    // 到期
    @Query("""
           select r from RecurringExpenseSchedule r
           where r.nextRunDate <= :today
             and (r.endDate is null or r.endDate >= :today)
           """)
    List<RecurringExpenseSchedule> findDueNoStatus(@Param("today") LocalDate today);

    // 给“手工记账 isRecurring=true”使用：找同用户+同分类+同频率的候选计划
    @Query("""
           select r from RecurringExpenseSchedule r
           where r.user.user_id = :userId
             and r.category.categoryId = :categoryId
             and r.frequency = :freq
           """)
    List<RecurringExpenseSchedule> findByUserCategoryFrequency(@Param("userId") Integer userId,
                                                               @Param("categoryId") Integer categoryId,
                                                               @Param("freq") RecurringExpenseSchedule.Frequency freq);
}
