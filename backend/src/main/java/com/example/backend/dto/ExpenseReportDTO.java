package com.example.backend.dto;

import java.math.BigDecimal;

public class ExpenseReportDTO {

    private Integer year;
    private Integer periodValue;   // week or month
    private String categoryName;
    private Double totalAmount;

    public ExpenseReportDTO(Integer year, Integer periodValue, String categoryName, BigDecimal totalAmount) {
        this.year = year;
        this.periodValue = periodValue;
        this.categoryName = categoryName;
        this.totalAmount = totalAmount == null ? null : totalAmount.doubleValue();
    }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getPeriodValue() { return periodValue; }
    public void setPeriodValue(Integer periodValue) { this.periodValue = periodValue; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
}
