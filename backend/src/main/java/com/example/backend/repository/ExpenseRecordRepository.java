package com.example.backend.repository;

import com.example.backend.dto.ExpenseReportDTO;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Integer>{

    List<ExpenseRecord> findByUser(User user);

    // 无关键字
    @Query(value = """
  SELECT e FROM ExpenseRecord e
  WHERE e.user.user_id = :userId
    AND e.expenseDate >= COALESCE(:fromDate, e.expenseDate)
    AND e.expenseDate <= COALESCE(:toDate,   e.expenseDate)
    AND e.category.categoryId = COALESCE(:categoryId, e.category.categoryId)
    AND e.isRecurring         = COALESCE(:recurring,  e.isRecurring)
  """,
            countQuery = """
  SELECT COUNT(e) FROM ExpenseRecord e
  WHERE e.user.user_id = :userId
    AND e.expenseDate >= COALESCE(:fromDate, e.expenseDate)
    AND e.expenseDate <= COALESCE(:toDate,   e.expenseDate)
    AND e.category.categoryId = COALESCE(:categoryId, e.category.categoryId)
    AND e.isRecurring         = COALESCE(:recurring,  e.isRecurring)
  """)
    Page<ExpenseRecord> searchNoKeyword(
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("categoryId") Integer categoryId,
            @Param("recurring") Boolean recurring,
            Pageable pageable
    );

    // 有关键字
    @Query(value = """
  SELECT e FROM ExpenseRecord e
  WHERE e.user.user_id = :userId
    AND e.expenseDate >= COALESCE(:fromDate, e.expenseDate)
    AND e.expenseDate <= COALESCE(:toDate,   e.expenseDate)
    AND e.category.categoryId = COALESCE(:categoryId, e.category.categoryId)
    AND e.isRecurring         = COALESCE(:recurring,  e.isRecurring)
    AND (
         LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
      OR LOWER(COALESCE(e.notes, ''))       LIKE LOWER(CONCAT('%', :q, '%'))
    )
  """,
            countQuery = """
  SELECT COUNT(e) FROM ExpenseRecord e
  WHERE e.user.user_id = :userId
    AND e.expenseDate >= COALESCE(:fromDate, e.expenseDate)
    AND e.expenseDate <= COALESCE(:toDate,   e.expenseDate)
    AND e.category.categoryId = COALESCE(:categoryId, e.category.categoryId)
    AND e.isRecurring         = COALESCE(:recurring,  e.isRecurring)
    AND (
         LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
      OR LOWER(COALESCE(e.notes, ''))       LIKE LOWER(CONCAT('%', :q, '%'))
    )
  """)
    Page<ExpenseRecord> searchWithKeyword(
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("categoryId") Integer categoryId,
            @Param("q") String q,
            @Param("recurring") Boolean recurring,
            Pageable pageable
    );

    /** 取消某个计划时，批量把历史账单的计划外键置空并把 isRecurring=false */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ExpenseRecord e " +
            "set e.recurringSchedule = null, e.isRecurring = false " +
            "where e.recurringSchedule.id = :scheduleId")
    void detachSchedule(@Param("scheduleId") Integer scheduleId);

    // Weekly report for a specific year + week
    @Query("SELECT new com.example.backend.dto.ExpenseReportDTO(" +
            "CAST(YEAR(e.expenseDate) AS integer), " +
            "CAST(FUNCTION('date_part', 'week', e.expenseDate) AS integer), " +
            "c.categoryName, " +
            "SUM(e.amount)) " +
            "FROM ExpenseRecord e JOIN e.category c " +
            "WHERE e.user.user_id = :userId " +
            "AND YEAR(e.expenseDate) = :year " +
            "AND CAST(FUNCTION('date_part', 'week', e.expenseDate) AS integer) = :week " +
            "GROUP BY YEAR(e.expenseDate), CAST(FUNCTION('date_part', 'week', e.expenseDate) AS integer), c.categoryName")
    List<ExpenseReportDTO> getWeeklyReportFor(
            @Param("userId") Integer userId,
            @Param("year") Integer year,
            @Param("week") Integer week);

    // Monthly report for a specific year + month
    @Query("SELECT new com.example.backend.dto.ExpenseReportDTO(" +
            "YEAR(e.expenseDate), MONTH(e.expenseDate), c.categoryName, SUM(e.amount)) " +
            "FROM ExpenseRecord e JOIN e.category c " +
            "WHERE e.user.user_id = :userId " +
            "AND YEAR(e.expenseDate) = :year " +
            "AND MONTH(e.expenseDate) = :month " +
            "GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate), c.categoryName")
    List<ExpenseReportDTO> getMonthlyReportFor(
            @Param("userId") Integer userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    // Yearly report for a specific year
    @Query("SELECT new com.example.backend.dto.ExpenseReportDTO(" +
            "YEAR(e.expenseDate), NULL, c.categoryName, SUM(e.amount)) " +
            "FROM ExpenseRecord e JOIN e.category c " +
            "WHERE e.user.user_id = :userId " +
            "AND YEAR(e.expenseDate) = :year " +
            "GROUP BY YEAR(e.expenseDate), c.categoryName")
    List<ExpenseReportDTO> getYearlyReportFor(
            @Param("userId") Integer userId,
            @Param("year") Integer year);

    interface CategorySpend {
        Integer getCategoryId();
        String  getCategoryName();
        java.math.BigDecimal getAmount();
    }

    @Query("select c.categoryId as categoryId, c.categoryName as categoryName, sum(e.amount) as amount " +
            "from ExpenseRecord e join e.category c " +
            "where e.user.user_id = :userId " +
            "and YEAR(e.expenseDate) = :year " +
            "and MONTH(e.expenseDate) = :month " +
            "group by c.categoryId, c.categoryName")
    java.util.List<CategorySpend> findMonthlySpend(
            @Param("userId") Integer userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

//     track spending goal
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ExpenseRecord e
        WHERE e.user = :user
          AND e.expenseDate BETWEEN :start AND :end
          AND (:categoryId IS NULL OR e.category.categoryId = :categoryId)
    """)
    BigDecimal sumByUserAndWindowAndCategoryId(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("categoryId") Integer categoryId
    );

    @Query("SELECT COUNT(e) FROM ExpenseRecord e WHERE e.user.user_id = :userId")
    long countRecordsByUserId(@Param("userId") Integer userId);
    
}
