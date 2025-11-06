import React, { useState, useEffect } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Rectangle,
} from "recharts";
import {
  FaShoppingCart,
  FaTshirt,
  FaSubway,
  FaCoffee,
  FaUtensils,
  FaGamepad,
  FaLightbulb,
  FaFilePdf,
} from "react-icons/fa";
import { expenseRecordService } from "../services/api";
import jsPDF from "jspdf";

// Category icon mapping
const categoryIcons = {
  Food: <FaUtensils />,
  Shopping: <FaShoppingCart />,
  Transport: <FaSubway />,
  Entertainment: <FaGamepad />,
  Utilities: <FaLightbulb />,
  // Add more categories and corresponding icons
};

// Category color mapping
const categoryColors = {
  Food: "#FF6384",
  Shopping: "#36A2EB",
  Transport: "#FFCE56",
  Entertainment: "#4BC0C0",
  Utilities: "#9966FF",
};

const formatCurrency = (amount) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(amount);

export default function Reports() {
  const [reportType, setReportType] = useState("weekly");
  const [reportData, setReportData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Get current date information
  const currentDate = new Date();
  const currentYear = currentDate.getFullYear();
  const currentMonth = currentDate.getMonth() + 1; // JavaScript months are 0-11

  // Calculate current week number (1-52)
  const getWeekNumber = (date) => {
    const firstDayOfYear = new Date(date.getFullYear(), 0, 1);
    const pastDaysOfYear = (date - firstDayOfYear) / 86400000;
    return Math.ceil((pastDaysOfYear + firstDayOfYear.getDay() + 1) / 7);
  };

  const currentWeek = getWeekNumber(currentDate);

  // State variables
  const [selectedYear, setSelectedYear] = useState(currentYear);
  const [selectedWeek, setSelectedWeek] = useState(currentWeek);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);

  // Generate year options (current year and 4 years back)
  const yearOptions = [];
  for (let i = 0; i < 5; i++) {
    yearOptions.push(currentYear - i);
  }

  // Generate week options (1-52)
  const weekOptions = [];
  for (let i = 1; i <= 52; i++) {
    weekOptions.push(i);
  }

  // Generate month options (1-12)
  const monthOptions = [
    { value: 1, name: "January" },
    { value: 2, name: "February" },
    { value: 3, name: "March" },
    { value: 4, name: "April" },
    { value: 5, name: "May" },
    { value: 6, name: "June" },
    { value: 7, name: "July" },
    { value: 8, name: "August" },
    { value: 9, name: "September" },
    { value: 10, name: "October" },
    { value: 11, name: "November" },
    { value: 12, name: "December" },
  ];

  // Fetch report data
  const fetchReport = async () => {
    try {
      setLoading(true);
      setError(null);

      let response;

      switch (reportType) {
        case "weekly":
          response = await expenseRecordService.getWeeklyReport(
            selectedYear,
            selectedWeek
          );
          break;
        case "monthly":
          response = await expenseRecordService.getMonthlyReport(
            selectedYear,
            selectedMonth
          );
          break;
        case "yearly":
          response = await expenseRecordService.getYearlyReport(selectedYear);
          break;
        default:
          throw new Error("Invalid report type");
      }

      // Filter out categories with zero amount
      const filteredData = response.data.filter(
        (item) => item.totalAmount > 0
      );
      setReportData(filteredData);
    } catch (err) {
      console.error(`Error fetching ${reportType} report:`, err);
      setError("Failed to load report data. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [reportType, selectedYear, selectedWeek, selectedMonth]);

  // Format data for chart
  const chartData = reportData.map((item) => ({
    name: item.categoryName,
    amount: item.totalAmount,
    fill: categoryColors[item.categoryName] || "#8884d8", // Use category color or default color
    label: item.label, // For monthly and yearly reports
  }));

  // Get all possible categories, even if there are no expenses
  const allCategories = [
    "Food",
    "Shopping",
    "Transport",
    "Entertainment",
    "Utilities",
  ];

  // Create data for card display, including all categories (showing 0 for those with no expense)
  const categoryCards = allCategories.map((category) => {
    const found = reportData.find((item) => item.categoryName === category);
    return {
      name: category,
      amount: found ? found.totalAmount : 0,
      icon: categoryIcons[category] || <FaShoppingCart />,
    };
  });

  // Pure JavaScript PDF export method
  const exportToPDF = () => {
    try {
      setLoading(true);

      // Create PDF document
      const pdf = new jsPDF("portrait", "mm", "a4");
      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const margin = 20;
      let yPos = margin;

      // Add report title
      pdf.setFont("helvetica", "bold");
      pdf.setFontSize(16);
      pdf.text(getReportTitle(), margin, yPos);
      yPos += 10;

      // Add report subtitle/description
      pdf.setFont("helvetica", "normal");
      pdf.setFontSize(12);
      pdf.text(getChartTitle(), margin, yPos);
      yPos += 15;

      // Add export date
      pdf.setFontSize(10);
      pdf.setTextColor(100, 100, 100);
      pdf.text(`Generated on: ${new Date().toLocaleDateString()}`, margin, yPos);
      yPos += 15;

      // If no data, show a message
      if (chartData.length === 0) {
        pdf.setFont("helvetica", "italic");
        pdf.setTextColor(100, 100, 100);
        pdf.text(
          "No spending data available for the selected period.",
          margin,
          yPos
        );
        yPos += 10;
      } else {
        // Add expense summary table
        pdf.setFont("helvetica", "bold");
        pdf.setFontSize(12);
        pdf.setTextColor(0, 0, 0);
        pdf.text("Spending Summary", margin, yPos);
        yPos += 8;

        // Table header row
        const tableColumnWidths = [
          (pageWidth - 2 * margin) * 0.6,
          (pageWidth - 2 * margin) * 0.4,
        ];
        const tableHeaders = ["Category", "Amount"];

        pdf.setFillColor(240, 240, 240);
        pdf.rect(margin, yPos, pageWidth - 2 * margin, 8, "F");

        pdf.setFont("helvetica", "bold");
        pdf.setFontSize(10);
        pdf.setTextColor(0, 0, 0);
        pdf.text(tableHeaders[0], margin + 3, yPos + 5);
        pdf.text(
          tableHeaders[1],
          margin + tableColumnWidths[0] + 3,
          yPos + 5
        );

        yPos += 8;

        // Add total
        let totalSpending = 0;

        // Table data rows
        categoryCards
          .filter((category) => category.amount > 0) // Only show categories with expenses
          .forEach((category, index) => {
            totalSpending += category.amount;

            // Alternating row background
            if (index % 2 === 1) {
              pdf.setFillColor(248, 248, 248);
              pdf.rect(margin, yPos, pageWidth - 2 * margin, 8, "F");
            }

            pdf.setFont("helvetica", "normal");
            pdf.text(category.name, margin + 3, yPos + 5);

            // Right-align amount
            const amountText = formatCurrency(category.amount);
            const amountWidth =
              (pdf.getStringUnitWidth(amountText) * 10) /
              pdf.internal.scaleFactor;
            pdf.text(
              amountText,
              margin +
                tableColumnWidths[0] +
                tableColumnWidths[1] -
                amountWidth -
                3,
              yPos + 5
            );

            yPos += 8;
          });

        // Add total row
        pdf.setFillColor(230, 230, 230);
        pdf.rect(margin, yPos, pageWidth - 2 * margin, 8, "F");

        pdf.setFont("helvetica", "bold");
        pdf.text("Total", margin + 3, yPos + 5);

        const totalText = formatCurrency(totalSpending);
        const totalWidth =
          (pdf.getStringUnitWidth(totalText) * 10) / pdf.internal.scaleFactor;
        pdf.text(
          totalText,
          margin +
            tableColumnWidths[0] +
            tableColumnWidths[1] -
            totalWidth -
            3,
          yPos + 5
        );

        yPos += 20;

        // Add visualization chart (simplified version)
        pdf.setFont("helvetica", "bold");
        pdf.setFontSize(12);
        pdf.text("Spending Visualization", margin, yPos);
        yPos += 10;

        // Find maximum amount to determine scale
        const maxAmount = Math.max(...chartData.map((item) => item.amount));
        const chartWidth = pageWidth - 2 * margin - 60; // Subtract space for labels and values

        // Draw bars for each category
        chartData.forEach((item, index) => {
          const barHeight = 8;
          const barWidth = Math.max(
            10,
            (item.amount / maxAmount) * chartWidth
          );
          const color = hexToRgb(item.fill);

          // Category name
          pdf.setFont("helvetica", "normal");
          pdf.setFontSize(9);
          pdf.setTextColor(0, 0, 0);
          pdf.text(item.name, margin, yPos + 4);

          // Draw bar
          pdf.setFillColor(color.r, color.g, color.b);
          pdf.rect(margin + 40, yPos, barWidth, barHeight, "F");

          // Display amount
          const amountText = formatCurrency(item.amount);
          pdf.setTextColor(0, 0, 0);
          pdf.text(amountText, margin + 45 + barWidth, yPos + 4);

          yPos += barHeight + 5;
        });
      }

      // Add footer
      yPos = pageHeight - margin;
      pdf.setFont("helvetica", "italic");
      pdf.setFontSize(8);
      pdf.setTextColor(150, 150, 150);
      pdf.text("Generated by Personal Finance Manager", margin, yPos);

      // Set filename
      let fileName = "";
      switch (reportType) {
        case "weekly":
          fileName = `Weekly_Report_Week${selectedWeek}_${selectedYear}.pdf`;
          break;
        case "monthly":
          fileName = `Monthly_Report_${
            monthOptions.find((m) => m.value === selectedMonth)?.name
          }_${selectedYear}.pdf`;
          break;
        case "yearly":
          fileName = `Yearly_Report_${selectedYear}.pdf`;
          break;
        default:
          fileName = `Expense_Report_${new Date()
            .toISOString()
            .split("T")[0]}.pdf`;
      }

      // Save PDF
      pdf.save(fileName);
    } catch (error) {
      console.error("Failed to export PDF:", error);
      setError("Failed to export PDF. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  // Helper function: convert hex color to RGB
  const hexToRgb = (hex) => {
    // Remove # if present
    hex = hex.replace("#", "");

    // Parse RGB values
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);

    return { r, g, b };
  };

  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      return (
          <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 shadow-lg">
            <p className="font-semibold text-gray-900 dark:text-gray-100">
            {payload[0].payload.name}
          </p>
            <p className="text-indigo-600 dark:text-indigo-400">{formatCurrency(payload[0].value)}</p>
        </div>
      );
    }
    return null;
  };

  // Render time selector
  const renderTimeSelector = () => {
    switch (reportType) {
      case "weekly":
        return (
          <>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Year:</label>
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(parseInt(e.target.value))}
          className="rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {yearOptions.map((year) => (
                  <option key={year} value={year}>
                    {year}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Week:</label>
              <select
                value={selectedWeek}
                onChange={(e) => setSelectedWeek(parseInt(e.target.value))}
          className="rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {weekOptions.map((week) => (
                  <option key={week} value={week}>
                    {week}
                  </option>
                ))}
              </select>
            </div>
          </>
        );
      case "monthly":
        return (
          <>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Year:</label>
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(parseInt(e.target.value))}
          className="rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {yearOptions.map((year) => (
                  <option key={year} value={year}>
                    {year}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Month:</label>
              <select
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(parseInt(e.target.value))}
          className="rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {monthOptions.map((month) => (
                  <option key={month.value} value={month.value}>
                    {month.name}
                  </option>
                ))}
              </select>
            </div>
          </>
        );
      case "yearly":
        return (
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-gray-700">Year:</label>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value))}
          className="rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>
        );
      default:
        return null;
    }
  };

  // Get report title
  const getReportTitle = () => {
    switch (reportType) {
      case "weekly":
        return "Weekly Reports";
      case "monthly":
        return "Monthly Reports";
      case "yearly":
        return "Yearly Reports";
      default:
        return "Reports";
    }
  };

  // Get chart title
  const getChartTitle = () => {
    switch (reportType) {
      case "weekly":
        return `Weekly Spending by Category (Week ${selectedWeek}, ${selectedYear})`;
      case "monthly":
        return `Monthly Spending by Category (${
          monthOptions.find((m) => m.value === selectedMonth)?.name
        } ${selectedYear})`;
      case "yearly":
        return `Yearly Spending by Category (${selectedYear})`;
      default:
        return "Spending by Category";
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-4">
      <div className="mb-8 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400">
          Insights
        </span>
  <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-100">
          {getReportTitle()}
        </h1>
  <p className="text-gray-500 dark:text-gray-400">
          Understand your spending patterns across different time periods.
        </p>
      </div>

      {/* Report type selector */}
      <div className="mb-6">
  <div className="flex border-b border-gray-200 dark:border-gray-700">
          <button
            onClick={() => setReportType("weekly")}
            className={`py-3 px-6 font-medium text-sm ${
              reportType === "weekly"
                ? "border-b-2 border-indigo-600 text-indigo-600"
                : "text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
            }`}
          >
            Weekly
          </button>
          <button
            onClick={() => setReportType("monthly")}
            className={`py-3 px-6 font-medium text-sm ${
              reportType === "monthly"
                ? "border-b-2 border-indigo-600 text-indigo-600"
                : "text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
            }`}
          >
            Monthly
          </button>
          <button
            onClick={() => setReportType("yearly")}
            className={`py-3 px-6 font-medium text-sm ${
              reportType === "yearly"
                ? "border-b-2 border-indigo-600 text-indigo-600"
                : "text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
            }`}
          >
            Yearly
          </button>
        </div>
      </div>

      {/* Time selector section */}
      <div className="mb-6 flex flex-wrap items-center gap-4">
        {renderTimeSelector()}

        <div className="ml-auto flex gap-2">
          <button
            onClick={fetchReport}
            className="rounded-full bg-indigo-600 px-4 py-2 text-xs font-semibold uppercase tracking-wider text-white hover:bg-indigo-700"
          >
            Refresh Report
          </button>

          {/* Add PDF export button */}
          <button
            onClick={exportToPDF}
            disabled={loading || reportData.length === 0}
            className="flex items-center gap-2 rounded-full bg-gray-800 px-4 py-2 text-xs font-semibold uppercase tracking-wider text-white hover:bg-gray-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            <FaFilePdf /> Export PDF
          </button>
        </div>
      </div>

      {/* Loading and error states */}
      {loading && (
        <div className="flex h-60 items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent"></div>
        </div>
      )}

      {error && (
        <div className="my-8 rounded-lg bg-red-50 p-4 text-red-600">
          <p>{error}</p>
        </div>
      )}

      {/* Chart section */}
      {!loading && !error && (
        <>
          <div className="mb-6 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-800 p-6 shadow-sm">
            <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              {getChartTitle()}
            </h2>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={chartData}
                  margin={{
                    top: 20,
                    right: 30,
                    left: 20,
                    bottom: 20,
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis
                    tickFormatter={(value) => `$${value}`}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend />
                  <Bar
                    dataKey="amount"
                    name="Amount"
                    fill="#8884d8"
                    activeBar={<Rectangle fill="#6366F1" stroke="#4F46E5" />}
                    radius={[4, 4, 0, 0]}
                    barSize={40}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Category cards */}
          <div>
            <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              Spending Details
            </h2>
            <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
              {categoryCards.map((category) => (
                <div
                  key={category.name}
                  className="flex flex-col rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-800 p-4 shadow-sm"
                >
                  <div className="flex items-center gap-3 mb-2">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
                      {category.icon}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-900 dark:text-gray-100">
                        {category.name}
                      </p>
                      <p className="text-xs uppercase tracking-widest text-gray-500 dark:text-gray-400">
                        SPENT
                      </p>
                    </div>
                  </div>
                  <p className="mt-auto text-xl font-bold text-gray-900 dark:text-gray-100">
                    {formatCurrency(category.amount)}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
