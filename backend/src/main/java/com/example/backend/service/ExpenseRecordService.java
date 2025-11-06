package com.example.backend.service;

import com.example.backend.dto.ExpenseReportDTO;
import com.example.backend.model.Category;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.UserRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Paragraph;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import java.io.ByteArrayOutputStream;


@Service
public class ExpenseRecordService {

    private final ExpenseRecordRepository expenseRecordRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AchievementService achievementService;

    public ExpenseRecordService(ExpenseRecordRepository expenseRecordRepository, UserRepository userRepository, CategoryRepository categoryRepository, AchievementService achievementService) {
        this.expenseRecordRepository = expenseRecordRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.achievementService = achievementService;
    }

    public List<ExpenseRecord> getRecordsForUser(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return expenseRecordRepository.findByUser(user);
    }

    public ExpenseRecord createRecord(Integer userId, ExpenseRecord recordData) {
        User user = userRepository.findById(userId).orElseThrow();
        Category category = categoryRepository.findById(recordData.getCategory().getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        recordData.setUser(user);
        recordData.setCategory(category);
        ExpenseRecord saved = expenseRecordRepository.save(recordData);
        achievementService.checkFirstExpense(userId);
        achievementService.checkTenRecords(userId);
        return saved;
    }

    public ExpenseRecord updateRecord(Integer userId, Integer recordId, ExpenseRecord updatedData) {
        User user = userRepository.findById(userId).orElseThrow();
        ExpenseRecord existing = expenseRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        if (!existing.getUser().equals(user)) {
            throw new RuntimeException("Unauthorized");
        }

        if (updatedData.getAmount() != null) existing.setAmount(updatedData.getAmount());
        if (updatedData.getCurrency() != null) existing.setCurrency(updatedData.getCurrency());
        if (updatedData.getDescription() != null) existing.setDescription(updatedData.getDescription());
        if (updatedData.getNotes() != null) existing.setNotes(updatedData.getNotes());
        if (updatedData.getPaymentMethod() != null) existing.setPaymentMethod(updatedData.getPaymentMethod());
        if (updatedData.getIsRecurring() != null) existing.setIsRecurring(updatedData.getIsRecurring());

        existing.setUpdatedAt(LocalDateTime.now());
        return expenseRecordRepository.save(existing);
    }

    public void deleteRecord(Integer userId, Integer recordId) {
        User user = userRepository.findById(userId).orElseThrow();
        ExpenseRecord existing = expenseRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        if (!existing.getUser().equals(user)) {
            throw new RuntimeException("Unauthorized");
        }

        expenseRecordRepository.deleteById(recordId);
    }

    public Page<ExpenseRecord> search(
            Integer userId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer categoryId,
            String q,
            Boolean recurring,
            Pageable pageable
    ) {
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();
        if (qNorm == null) {
            return expenseRecordRepository.searchNoKeyword(
                    userId, fromDate, toDate, categoryId, recurring, pageable);
        } else {
            return expenseRecordRepository.searchWithKeyword(
                    userId, fromDate, toDate, categoryId, qNorm, recurring, pageable);
        }
    }

    // === Weekly Report ===
    public List<ExpenseReportDTO> getWeeklyReport(Integer userId, Integer year, Integer week) {
        return expenseRecordRepository.getWeeklyReportFor(userId, year, week);
    }

    // === Monthly Report ===
    public List<ExpenseReportDTO> getMonthlyReport(Integer userId, Integer year, Integer month) {
        return expenseRecordRepository.getMonthlyReportFor(userId, year, month);
    }

    // === Yearly Report ===
    public List<ExpenseReportDTO> getYearlyReport(Integer userId, Integer year) {
        return expenseRecordRepository.getYearlyReportFor(userId, year);
    }

    public byte[] exportReportPdf(Integer userId, String period, Integer year, Integer month, Integer week) {
        List<ExpenseReportDTO> reports;

        switch (period.toLowerCase()) {
            case "weekly" -> {
                if (year == null || week == null) {
                    throw new IllegalArgumentException("Year and week are required for weekly reports.");
                }
                reports = expenseRecordRepository.getWeeklyReportFor(userId, year, week);
            }
            case "monthly" -> {
                if (year == null || month == null) {
                    throw new IllegalArgumentException("Year and month are required for monthly reports.");
                }
                reports = expenseRecordRepository.getMonthlyReportFor(userId, year, month);
            }
            case "yearly" -> {
                if (year == null) {
                    throw new IllegalArgumentException("Year is required for yearly reports.");
                }
                reports = expenseRecordRepository.getYearlyReportFor(userId, year);
            }
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        }

        // Generate PDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(period.toUpperCase() + " Expense Report"));
            document.add(new Paragraph("Year: " + year +
                    (month != null ? ", Month: " + month : "") +
                    (week != null ? ", Week: " + week : "")));
            document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.addCell("Year");
            table.addCell("Period");
            table.addCell("Category");
            table.addCell("Amount");

            double total = 0.0;
            for (ExpenseReportDTO r : reports) {
                table.addCell(String.valueOf(r.getYear()));
                table.addCell(r.getPeriodValue() == null ? "-" : String.valueOf(r.getPeriodValue()));
                table.addCell(r.getCategoryName());
                table.addCell(r.getTotalAmount().toString());
                total += r.getTotalAmount().doubleValue();
            }

            // Add total row
            table.addCell("");
            table.addCell("");
            table.addCell("Total");
            table.addCell(String.valueOf(total));

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

}