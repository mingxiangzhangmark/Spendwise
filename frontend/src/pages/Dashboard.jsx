import React, { useEffect, useMemo, useState } from "react";
import { FaArrowRight } from "react-icons/fa";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { useNavigate } from "react-router-dom";
import { expenseRecordService, goalsService } from "../services/api";
import { useAuth } from "../context/AuthContext";

const TIME_RANGES = ["Day", "Week", "Month", "Year"];
const MS_IN_DAY = 24 * 60 * 60 * 1000;

const formatCurrency = (amount) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: amount % 1 === 0 ? 0 : 2,
  }).format(amount);

const formatPeriodLabel = (period) => {
  switch (period) {
    case "WEEKLY":
      return "Weekly";
    case "MONTHLY":
      return "Monthly";
    case "YEARLY":
      return "Yearly";
    default:
      return "Goal";
  }
};

const parseNumber = (value) => {
  if (value === null || value === undefined) {
    return 0;
  }
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : 0;
  }
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : 0;
};

const toDateKey = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const parseISODate = (value) => {
  if (!value || typeof value !== "string") {
    return null;
  }
  const [year, month, day] = value.split("-").map(Number);
  if (
    Number.isNaN(year) ||
    Number.isNaN(month) ||
    Number.isNaN(day)
  ) {
    return null;
  }
  return new Date(year, month - 1, day);
};

const addDays = (date, amount) => {
  const cloned = new Date(date);
  cloned.setDate(cloned.getDate() + amount);
  return cloned;
};

const startOfWeek = (date) => {
  const cloned = new Date(date);
  const day = cloned.getDay(); // 0 (Sun) - 6 (Sat)
  const diff = (day + 6) % 7; // convert to Monday start
  cloned.setDate(cloned.getDate() - diff);
  return cloned;
};

const getMonthKey = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  return `${year}-${month}`;
};

const getISOWeek = (date) => {
  const tempDate = new Date(date.getTime());
  tempDate.setHours(0, 0, 0, 0);
  tempDate.setDate(tempDate.getDate() + 3 - ((tempDate.getDay() + 6) % 7));
  const week1 = new Date(tempDate.getFullYear(), 0, 4);
  const weekNumber = Math.round(
    ((tempDate.getTime() - week1.getTime()) / MS_IN_DAY - 3 + ((week1.getDay() + 6) % 7)) / 7 + 1,
  );
  return { week: weekNumber, year: tempDate.getFullYear() };
};

const daysBetweenInclusive = (start, end) => {
  if (!start || !end) {
    return 0;
  }
  const diff = Math.floor((end.getTime() - start.getTime()) / MS_IN_DAY);
  return diff >= 0 ? diff + 1 : 0;
};

const clampPercentage = (value) => {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Math.max(0, Math.min(150, value));
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, isAuthenticated, loading: authLoading } = useAuth();

  const [timeRange, setTimeRange] = useState("Month");
  const [records, setRecords] = useState([]);
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const today = useMemo(() => new Date(), []);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate("/login", {
        state: { message: "Please log in to view your dashboard" },
      });
    }
  }, [authLoading, isAuthenticated, navigate]);

  useEffect(() => {
    if (!isAuthenticated || authLoading) {
      return;
    }

    let cancelled = false;

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [recordsResult, goalsResult] = await Promise.allSettled([
          expenseRecordService.getAllRecords(),
          goalsService.listProgress(),
        ]);

        const unauthorized =
          [recordsResult, goalsResult].some(
            (result) =>
              result.status === "rejected" &&
              (result.reason?.response?.status === 401 ||
                result.reason?.response?.status === 403),
          );

        if (unauthorized) {
          if (!cancelled) {
            navigate("/login", {
              state: { message: "Session expired. Please log in again." },
            });
          }
          return;
        }

        let recordsData = [];
        let goalsData = [];
        const errors = [];

        if (recordsResult.status === "fulfilled" && Array.isArray(recordsResult.value)) {
          recordsData = recordsResult.value;
        } else if (recordsResult.status === "rejected") {
          errors.push("transactions");
          console.error("Failed to load transactions:", recordsResult.reason);
        }

        if (goalsResult.status === "fulfilled" && Array.isArray(goalsResult.value)) {
          goalsData = goalsResult.value;
        } else if (goalsResult.status === "rejected") {
          errors.push("goals");
          console.error("Failed to load goals:", goalsResult.reason);
        }

        if (!cancelled) {
          setRecords(recordsData);
          setGoals(goalsData);
          if (errors.length === 2) {
            setError("We couldn't load your dashboard data right now. Please try again shortly.");
          } else if (errors.length === 1) {
            setError(`Some dashboard information (${errors[0]}) could not be loaded.`);
          } else {
            setError(null);
          }
        }
      } catch (fetchError) {
        console.error("Unexpected dashboard fetch error:", fetchError);
        if (!cancelled) {
          setError("We couldn't load your dashboard data right now. Please try again shortly.");
          setRecords([]);
          setGoals([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, [authLoading, isAuthenticated, navigate]);

  const normalizedRecords = useMemo(() => {
    if (!Array.isArray(records)) {
      return [];
    }
    return records
      .map((record) => {
        const amount = parseNumber(record.amount);
        const date = record.expenseDate ?? record.date;
        const parsedDate = parseISODate(date);
        if (!parsedDate || amount <= 0) {
          return null;
        }
        return {
          id: record.expenseId ?? record.id,
          amount,
          currency: record.currency ?? "USD",
          date: toDateKey(parsedDate),
          category: record.category?.name ?? "Expense",
          description: record.description ?? "",
        };
      })
      .filter(Boolean);
  }, [records]);

  const goalSummaries = useMemo(() => {
    if (!Array.isArray(goals)) {
      return [];
    }

    return goals
      .map((goal) => {
        const startDate = parseISODate(goal.startDate);
        const endDate = parseISODate(goal.endDate);
        if (startDate && endDate && endDate.getTime() < startDate.getTime()) {
          return null;
        }

        const targetAmount = parseNumber(goal.targetAmount);
        const spentAmount = parseNumber(goal.spentAmount);
        const remainingAmount = parseNumber(goal.remainingAmount);
        const progressPercent = clampPercentage(parseNumber(goal.progressPercent));
        const durationDays = daysBetweenInclusive(startDate, endDate);
        const dailyBudget =
          durationDays > 0 && targetAmount > 0 ? targetAmount / durationDays : 0;

        return {
          goalId: goal.goalId,
          displayName: `${goal.categoryName ?? "Goal"} ${formatPeriodLabel(goal.period)}`,
          categoryName: goal.categoryName ?? "Goal",
          period: goal.period,
          startDate,
          endDate,
          targetAmount,
          spentAmount,
          remainingAmount,
          progressPercent,
          dailyBudget,
          health: goal.health ?? "ON_TRACK",
        };
      })
      .filter(Boolean)
      .sort((a, b) => {
        if (!a.startDate || !b.startDate) {
          return 0;
        }
        return a.startDate.getTime() - b.startDate.getTime();
      });
  }, [goals]);

  const spentByDate = useMemo(() => {
    const map = new Map();
    normalizedRecords.forEach((record) => {
      map.set(record.date, (map.get(record.date) ?? 0) + record.amount);
    });
    return map;
  }, [normalizedRecords]);

  const monthlySpendMap = useMemo(() => {
    const map = new Map();
    normalizedRecords.forEach((record) => {
      const [year, month] = record.date.split("-").map(Number);
      if (Number.isNaN(year) || Number.isNaN(month)) {
        return;
      }
      const key = `${String(year)}-${String(month).padStart(2, "0")}`;
      map.set(key, (map.get(key) ?? 0) + record.amount);
    });
    return map;
  }, [normalizedRecords]);

  const currentMonthKey = useMemo(() => getMonthKey(today), [today]);
  const currentMonthSpend = monthlySpendMap.get(currentMonthKey) ?? 0;

  const topCategoriesCurrentMonth = useMemo(() => {
    const prefix = `${currentMonthKey}-`;
    const categoryTotals = new Map();
    normalizedRecords.forEach((record) => {
      if (record.date.startsWith(prefix)) {
        categoryTotals.set(
          record.category,
          (categoryTotals.get(record.category) ?? 0) + record.amount,
        );
      }
    });
    const entries = Array.from(categoryTotals.entries())
      .map(([name, total]) => ({
        name,
        total,
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 5)
      .map((entry) => ({
        ...entry,
        percent: currentMonthSpend > 0 ? (entry.total / currentMonthSpend) * 100 : 0,
      }));
    return entries;
  }, [currentMonthKey, currentMonthSpend, normalizedRecords]);

  const lastSevenDaysStats = useMemo(() => {
    const todayKey = toDateKey(today);
    const endDate = parseISODate(todayKey);
    const startDate = addDays(endDate, -6);
    const prevStartDate = addDays(startDate, -7);
    const prevEndDate = addDays(startDate, -1);
    const startKey = toDateKey(startDate);
    const endKey = toDateKey(endDate);
    const prevStartKey = toDateKey(prevStartDate);
    const prevEndKey = toDateKey(prevEndDate);

    let current = 0;
    let previous = 0;

    normalizedRecords.forEach((record) => {
      const date = record.date;
      if (date >= startKey && date <= endKey) {
        current += record.amount;
      } else if (date >= prevStartKey && date <= prevEndKey) {
        previous += record.amount;
      }
    });

    const change =
      previous === 0
        ? null
        : Number.isFinite((current - previous) / previous)
          ? ((current - previous) / previous) * 100
          : null;

    return {
      current,
      previous,
      change,
    };
  }, [normalizedRecords, today]);

  const lastThirtyDaysStats = useMemo(() => {
    const todayKey = toDateKey(today);
    const endDate = parseISODate(todayKey);
    const startDate = addDays(endDate, -29);
    const startKey = toDateKey(startDate);
    const endKey = toDateKey(endDate);
    let total = 0;
    const activeDays = new Set();

    normalizedRecords.forEach((record) => {
      if (record.date >= startKey && record.date <= endKey) {
        total += record.amount;
        activeDays.add(record.date);
      }
    });

    const activeDayCount = activeDays.size || 1;

    return {
      total,
      activeDays: activeDays.size,
      average: total === 0 ? 0 : total / activeDayCount,
    };
  }, [normalizedRecords, today]);

  const goalHealthSummary = useMemo(() => {
    const summary = {
      active: goalSummaries.length,
      onTrack: 0,
      atRisk: 0,
      overspent: 0,
    };

    goalSummaries.forEach((goal) => {
      if (goal.health === "OVERSPENT") {
        summary.overspent += 1;
      } else if (goal.health === "AT_RISK") {
        summary.atRisk += 1;
      } else {
        summary.onTrack += 1;
      }
    });

    return summary;
  }, [goalSummaries]);

  const averageGoalProgress = useMemo(() => {
    const validGoals = goalSummaries.filter((goal) =>
      Number.isFinite(goal.progressPercent),
    );
    if (!validGoals.length) {
      return 0;
    }
    const total = validGoals.reduce((sum, goal) => sum + goal.progressPercent, 0);
    return Math.round(total / validGoals.length);
  }, [goalSummaries]);

  const goalsForDisplay = useMemo(() => {
    return goalSummaries.map((goal) => ({
      id: goal.goalId,
      name: goal.displayName,
      amount: goal.targetAmount,
      progressLabel: Math.round(goal.progressPercent),
      progressBar: Math.min(100, Math.max(0, goal.progressPercent)),
    }));
  }, [goalSummaries]);

  const recentTransactions = useMemo(() => {
    return [...normalizedRecords]
      .sort((a, b) => {
        if (a.date === b.date) {
          return (b.id ?? 0) - (a.id ?? 0);
        }
        return parseISODate(b.date) - parseISODate(a.date);
      })
      .slice(0, 6)
      .map((record) => ({
        id: record.id,
        name: record.description || record.category,
        amount: -record.amount,
        type: record.category,
        date: record.date,
      }));
  }, [normalizedRecords]);

  const chartSeries = useMemo(() => {
    const buildDaySeries = () => {
      const data = [];
      for (let offset = 6; offset >= 0; offset--) {
        const date = addDays(today, -offset);
        const key = toDateKey(date);
        data.push({
          name: date.toLocaleDateString("en-US", { weekday: "short" }),
          spend: spentByDate.get(key) ?? 0,
        });
      }
      return data;
    };

    const buildWeekSeries = () => {
      const data = [];
      const currentWeekStart = startOfWeek(today);
      for (let offset = 5; offset >= 0; offset--) {
        const weekStart = addDays(currentWeekStart, -offset * 7);
        const weekEnd = addDays(weekStart, 6);
        let spend = 0;
        for (
          let cursor = new Date(weekStart);
          cursor <= weekEnd;
          cursor = addDays(cursor, 1)
        ) {
          const key = toDateKey(cursor);
          spend += spentByDate.get(key) ?? 0;
        }
        data.push({
          name: `Wk ${getISOWeek(weekStart).week}`,
          spend,
        });
      }
      return data;
    };

    const buildMonthSeries = () => {
      const data = [];
      const reference = new Date(today.getFullYear(), today.getMonth(), 1);
      for (let offset = 5; offset >= 0; offset--) {
        const monthStart = new Date(reference.getFullYear(), reference.getMonth() - offset, 1);
        const monthEnd = new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0);
        let spend = 0;
        for (
          let cursor = new Date(monthStart);
          cursor <= monthEnd;
          cursor = addDays(cursor, 1)
        ) {
          const key = toDateKey(cursor);
          spend += spentByDate.get(key) ?? 0;
        }
        data.push({
          name: monthStart.toLocaleDateString("en-US", { month: "short" }),
          spend,
        });
      }
      return data;
    };

    const buildYearSeries = () => {
      const data = [];
      for (let monthIndex = 0; monthIndex < 12; monthIndex++) {
        const monthStart = new Date(today.getFullYear(), monthIndex, 1);
        const monthEnd = new Date(today.getFullYear(), monthIndex + 1, 0);
        let spend = 0;
        for (
          let cursor = new Date(monthStart);
          cursor <= monthEnd;
          cursor = addDays(cursor, 1)
        ) {
          const key = toDateKey(cursor);
          spend += spentByDate.get(key) ?? 0;
        }
        data.push({
          name: monthStart.toLocaleDateString("en-US", { month: "short" }),
          spend,
        });
      }
      return data;
    };

    return {
      Day: buildDaySeries(),
      Week: buildWeekSeries(),
      Month: buildMonthSeries(),
      Year: buildYearSeries(),
    };
  }, [spentByDate, today]);

  const selectedSeries = chartSeries[timeRange] ?? [];

  const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload || !payload.length) {
      return null;
    }
    const spendPoint = payload[0];
    return (
      <div className="rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-xs shadow-md">
        <p className="font-medium text-gray-900 dark:text-white">{label}</p>
        <p className="text-rose-500">
          Spend: {formatCurrency(spendPoint?.value ?? 0)}
        </p>
      </div>
    );
  };

  const lastSevenChange = lastSevenDaysStats.change;
  const lastSevenChangeLabel =
    lastSevenChange === null ? "New activity" : `${Math.abs(lastSevenChange).toFixed(1)}%`;
  const lastSevenChangeSymbol =
    lastSevenChange === null ? "•" : lastSevenChange > 0 ? "↑" : "↓";
  const lastSevenChangeClass =
    lastSevenChange === null
      ? "text-gray-500 dark:text-gray-400"
      : lastSevenChange > 0
        ? "text-rose-500"
        : "text-emerald-500";
  const lastSevenChangeCaption =
    lastSevenChange === null
      ? "No spending the prior week"
      : lastSevenChange > 0
        ? "Higher than previous week"
        : "Lower than previous week";

  const averageDailySpend = lastThirtyDaysStats.average;
  const activeDaysCount = lastThirtyDaysStats.activeDays;

  const userName = user?.username ?? user?.email ?? "there";

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-10 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Overview</span>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-white">Dashboard</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Welcome back, {userName}. Here's a look at your money today.
        </p>
      </div>

      {error && (
        <div className="mb-6 rounded-3xl border border-amber-200 bg-amber-50 px-6 py-4 text-sm text-amber-700 dark:border-amber-400 dark:bg-amber-900/30 dark:text-amber-200">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-10 text-center text-sm text-gray-500 dark:text-gray-400 shadow-sm">
          Loading your dashboard...
        </div>
      ) : (
        <>
          <div className="mb-10 grid gap-4 sm:grid-cols-3">
            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
              <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Last 7 Days</p>
              <div className="mt-4 flex items-end justify-between">
                <p className="text-3xl font-semibold text-gray-900 dark:text-white">
                  {formatCurrency(lastSevenDaysStats.current)}
                </p>
                <span
                  className={`text-xs font-medium uppercase tracking-[0.3em] ${lastSevenChangeClass}`}
                >
                  {lastSevenChangeSymbol} {lastSevenChangeLabel}
                </span>
              </div>
              <p className="mt-3 text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                {lastSevenChangeCaption}
              </p>
            </div>

            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
              <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Daily Average (30d)</p>
              <div className="mt-4 flex items-end justify-between">
                <p className="text-3xl font-semibold text-gray-900 dark:text-white">
                  {formatCurrency(averageDailySpend)}
                </p>
                <span className="text-xs font-medium uppercase tracking-[0.3em] text-gray-500 dark:text-gray-400">
                  {activeDaysCount || 0} active days
                </span>
              </div>
              <p className="mt-3 text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                {`Total ${formatCurrency(lastThirtyDaysStats.total)} in 30 days`}
              </p>
            </div>

            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
              <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Active Goals</p>
              <div className="mt-4 flex items-end justify-between">
                <p className="text-3xl font-semibold text-gray-900 dark:text-white">
                  {goalHealthSummary.active}
                </p>
                <span className="text-xs font-medium uppercase tracking-[0.3em] text-gray-500 dark:text-gray-400">
                  {goalHealthSummary.onTrack} on track
                </span>
              </div>
              <p className="mt-3 text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                {`At risk ${goalHealthSummary.atRisk} • Over ${goalHealthSummary.overspent}`}
              </p>
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm lg:col-span-2 transition-colors duration-200">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Spending</p>
                  <h2 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Spending Trend</h2>
                </div>
                <div className="flex gap-2">
                  {TIME_RANGES.map((range) => (
                    <button
                      key={range}
                      type="button"
                      onClick={() => setTimeRange(range)}
                      className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.2em] transition-colors ${
                        timeRange === range
                          ? "bg-gray-900 dark:bg-gray-600 text-white"
                          : "bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                      }`}
                    >
                      {range}
                    </button>
                  ))}
                </div>
              </div>

              <div className="mt-8 h-72 rounded-2xl border border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-2 transition-colors duration-200">
                {selectedSeries.length ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                      data={selectedSeries}
                      margin={{ top: 10, right: 10, left: 0, bottom: 5 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.3} />
                      <XAxis
                        dataKey="name"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: 10 }}
                      />
                      <YAxis
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: 10 }}
                        tickFormatter={(value) => formatCurrency(value)}
                      />
                      <Tooltip content={<CustomTooltip />} />
                      <Line
                        type="monotone"
                        dataKey="spend"
                        stroke="#EF4444"
                        strokeWidth={2}
                        dot={false}
                        activeDot={{ r: 5 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex h-full items-center justify-center text-sm text-gray-500 dark:text-gray-400">
                    No spending data available yet.
                  </div>
                )}
              </div>

              <div className="mt-4 flex justify-center gap-6 text-xs">
                <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
                  <span className="inline-block h-[2px] w-6 bg-rose-500" />
                  <span className="tracking-[0.3em]">Spend</span>
                </div>
              </div>
            </div>

            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
              <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Activity</p>
              <h2 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Recent Transactions</h2>
              {recentTransactions.length ? (
                <>
                  <ul className="mt-6 divide-y divide-gray-100 dark:divide-gray-700">
                    {recentTransactions.map((transaction) => (
                      <li key={transaction.id}>
                        <button
                          type="button"
                          onClick={() => navigate("/expense")}
                          className="flex w-full items-center justify-between gap-4 py-4 text-left transition-colors hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gray-400 dark:hover:bg-gray-700/50"
                        >
                          <div>
                            <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                              {transaction.type}
                            </p>
                            <p className="text-base font-semibold text-gray-900 dark:text-white">
                              {transaction.name || "Expense"}
                            </p>
                            <p className="text-xs text-gray-400 dark:text-gray-500">
                              {transaction.date}
                            </p>
                          </div>
                          <span
                            className={`text-base font-semibold ${
                              transaction.amount >= 0 ? "text-emerald-500" : "text-rose-500"
                            }`}
                          >
                            {transaction.amount >= 0 ? "+" : "-"}
                            {formatCurrency(Math.abs(transaction.amount))}
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                  <div className="mt-4 flex justify-end">
                    <button
                      type="button"
                      onClick={() => navigate("/expense")}
                      className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.3em] text-gray-900 transition-colors hover:text-gray-600 dark:text-gray-200 dark:hover:text-gray-400"
                    >
                      View expenses
                      <FaArrowRight className="h-3 w-3" />
                    </button>
                  </div>
                </>
              ) : (
                <div className="mt-6 rounded-2xl border border-dashed border-gray-200 dark:border-gray-700 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                  No transactions recorded yet.
                </div>
              )}
            </div>
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-3">
            <div className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm lg:col-span-2 transition-colors duration-200">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Goals</p>
                  <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Current Goals</h2>
                </div>
                <div className="text-right">
                  <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Average Progress</p>
                  <p className="text-xl font-semibold text-gray-900 dark:text-white">{averageGoalProgress}%</p>
                </div>
              </div>

              {goalsForDisplay.length ? (
                <ul className="mt-6 space-y-4">
                  {goalsForDisplay.map((goal) => (
                    <li
                      key={goal.id}
                      className="rounded-2xl border border-gray-100 dark:border-gray-700 p-4 bg-gray-50 dark:bg-gray-900 transition-colors duration-200"
                    >
                      <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
                        <span className="uppercase tracking-[0.2em]">{goal.name}</span>
                        <span className="text-gray-900 dark:text-white">
                          {formatCurrency(goal.amount)}
                        </span>
                      </div>
                      <div className="mt-3 h-2 w-full rounded-full bg-gray-100 dark:bg-gray-800">
                        <div
                          className="h-full rounded-full bg-gray-900 dark:bg-gray-600"
                          style={{ width: `${goal.progressBar}%` }}
                        />
                      </div>
                      <p className="mt-2 text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                        {goal.progressLabel}% completed
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="mt-6 rounded-2xl border border-dashed border-gray-200 dark:border-gray-700 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                  No active goals yet. Create one to start tracking your progress.
                </div>
              )}
            </div>

            <div className="flex flex-col rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
              <div>
                <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Insights</p>
                <h2 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Top Categories</h2>
                <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">
                  Your biggest spending areas this month.
                </p>
              </div>
              {topCategoriesCurrentMonth.length ? (
                <ul className="mt-6 space-y-4">
                  {topCategoriesCurrentMonth.map((category) => {
                    const share = Math.round(category.percent);
                    return (
                      <li key={category.name}>
                        <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
                          <span className="uppercase tracking-[0.2em]">{category.name}</span>
                          <span className="text-sm font-semibold text-gray-900 dark:text-white">
                            {formatCurrency(category.total)}
                          </span>
                        </div>
                        <div className="mt-2 h-2 w-full rounded-full bg-gray-100 dark:bg-gray-800">
                          <div
                            className="h-full rounded-full bg-rose-500"
                            style={{ width: `${Math.min(100, Math.max(share, 0))}%` }}
                          />
                        </div>
                        <p className="mt-2 text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                          {share}% of monthly spend
                        </p>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <div className="mt-6 rounded-2xl border border-dashed border-gray-200 dark:border-gray-700 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                  Start logging expenses to discover where your money goes.
                </div>
              )}
              <div className="mt-6">
                <button
                  type="button"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-gray-900 dark:bg-gray-700 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-gray-600"
                  onClick={() => navigate("/expense")}
                >
                  Explore expenses
                  <FaArrowRight />
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
