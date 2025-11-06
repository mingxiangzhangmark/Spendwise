import React, { useCallback, useEffect, useMemo, useState } from "react";
import { FaChevronDown, FaEdit, FaExclamationTriangle, FaPlus, FaTimes } from "react-icons/fa";
import { goalsService } from "../services/api";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const DEFAULT_CURRENCY = "AUD";

const DEFAULT_CATEGORIES = [
  { id: 1, name: "Food" },
  { id: 2, name: "Transport" },
  { id: 3, name: "Entertainment" },
  { id: 4, name: "Shopping" },
  { id: 5, name: "Utilities" },
];

const PERIOD_OPTIONS = [
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "YEARLY", label: "Yearly" },
];

const LABEL_CLASSES = "text-sm uppercase tracking-[0.3em] text-gray-400";
const INPUT_BASE_CLASSES =
  "w-full bg-transparent py-3 text-base text-gray-900 dark:text-white placeholder:text-gray-400 focus:outline-none";
const BORDER_SECTION_CLASSES = "mt-3 border-b border-gray-200 dark:border-gray-600";

const parseNumber = (value) => {
  if (value === null || value === undefined) {
    return 0;
  }
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : 0;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const formatCurrency = (value, currency = DEFAULT_CURRENCY) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(parseNumber(value));

const formatDate = (value) => {
  if (!value) {
    return "—";
  }
  const date = new Date(`${value}T00:00:00`);
  return date.toLocaleDateString("en-AU", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
};

const formatPeriodLabel = (period) => {
  switch (period) {
    case "WEEKLY":
      return "Weekly Goal";
    case "MONTHLY":
      return "Monthly Goal";
    case "YEARLY":
      return "Yearly Goal";
    default:
      return "Goal";
  }
};

const HEALTH_LABELS = {
  ON_TRACK: "On track",
  AT_RISK: "At risk",
  OVERSPENT: "Overspent",
};

const HEALTH_STYLES = {
  ON_TRACK: "bg-emerald-100 text-emerald-600",
  AT_RISK: "bg-amber-100 text-amber-600",
  OVERSPENT: "bg-rose-100 text-rose-600",
};

const ALERT_LABELS = {
  WARNING: "Approaching target",
  OVER_BUDGET: "Exceeded target",
};

const ALERT_STYLES = {
  WARNING: "text-amber-600 dark:text-amber-400",
  OVER_BUDGET: "text-rose-600 dark:text-rose-400",
};

const createEmptyFormState = (categories = DEFAULT_CATEGORIES) => ({
  categoryId: categories[0]?.id ? String(categories[0].id) : "",
  goalName: categories[0]?.name ? `${categories[0].name} Goal` : "",
  period: "MONTHLY",
  targetAmount: "",
  startNextPeriod: false,
  confirmDuplicate: false,
});

export default function Goals() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const navigate = useNavigate();

  const [goals, setGoals] = useState([]);
  const [categories, setCategories] = useState(DEFAULT_CATEGORIES);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(() => createEmptyFormState(DEFAULT_CATEGORIES));
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [editingGoalId, setEditingGoalId] = useState(null);
  const [goalNameEdited, setGoalNameEdited] = useState(false);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate("/login", {
        state: { message: "Please log in to manage your spending goals" },
      });
    }
  }, [authLoading, isAuthenticated, navigate]);

  useEffect(() => {
    setForm((prev) => {
      if (!categories.length) {
        return { ...prev, categoryId: "", goalName: "" };
      }
      const exists = categories.some(
        (category) => String(category.id) === String(prev.categoryId),
      );
      if (exists) {
        return prev;
      }
      if (editingGoalId || goalNameEdited) {
        return prev;
      }
      const defaultName = `${categories[0].name} Goal`;
      return {
        ...prev,
        categoryId: String(categories[0].id),
        goalName: defaultName,
      };
    });
  }, [categories, editingGoalId, goalNameEdited]);

  const fetchGoals = useCallback(async () => {
    try {
      setLoading(true);
      setFetchError(null);

      const [progressResponse, activeResponse] = await Promise.allSettled([
        goalsService.listProgress(),
        goalsService.listActiveGoals(),
      ]);

      const isUnauthorized = [progressResponse, activeResponse].some(
        (result) =>
          result.status === "rejected" &&
          (result.reason?.response?.status === 401 ||
            result.reason?.response?.status === 403),
      );

      if (isUnauthorized) {
        navigate("/login", {
          state: { message: "Session expired. Please log in again." },
        });
        return;
      }

      const progressRejected = progressResponse.status === "rejected";
      const activeRejected = activeResponse.status === "rejected";

      if (progressRejected && activeRejected) {
        console.error("Failed to load any goal data", {
          progressError: progressResponse.reason,
          activeError: activeResponse.reason,
        });
        setFetchError("Unable to load goals right now. Please try again shortly.");
        setGoals([]);
        return;
      }

      if (progressRejected || activeRejected) {
        console.warn("Partial goal data loaded", {
          progressError: progressResponse.reason,
          activeError: activeResponse.reason,
        });
        setFetchError(
          "Some goal information could not be loaded. Showing available data.",
        );
      } else {
        setFetchError(null);
      }

      const progressData =
        progressResponse.status === "fulfilled" ? progressResponse.value : [];
      const activeGoals =
        activeResponse.status === "fulfilled" ? activeResponse.value : [];

      const progressMap = new Map(progressData.map((item) => [item.goalId, item]));

      const combined = activeGoals.map((goal) => {
        const progress = progressMap.get(goal.goalId);
        const categoryName = goal.categoryName ?? progress?.categoryName ?? "Unknown";
        const derivedGoalName = goal.goalName ?? progress?.goalName ?? categoryName;

        return {
          goalId: goal.goalId,
          categoryId: goal.categoryId,
          categoryName,
          goalName: derivedGoalName,
          period: goal.period ?? progress?.period ?? "MONTHLY",
          targetAmount: parseNumber(progress?.targetAmount ?? goal.targetAmount),
          spentAmount: parseNumber(progress?.spentAmount),
          remainingAmount: parseNumber(progress?.remainingAmount ?? goal.targetAmount),
          progressPercent: parseNumber(progress?.progressPercent),
          daysLeft: progress?.daysLeft ?? 0,
          health: progress?.health ?? "ON_TRACK",
          alertLevel: progress?.alertLevel ?? "NONE",
          startDate: progress?.startDate ?? null,
          endDate: progress?.endDate ?? null,
          warningThreshold: progress?.warningThreshold ?? 80,
          overBudgetThreshold: progress?.overBudgetThreshold ?? 100,
        };
      });

      progressData.forEach((progress) => {
        const alreadyIncluded = combined.some((goal) => goal.goalId === progress.goalId);

        if (!alreadyIncluded) {
          const categoryName = progress.categoryName ?? "Unknown";
          combined.push({
            goalId: progress.goalId,
            categoryId: null,
            categoryName,
            goalName: progress.goalName ?? categoryName,
            period: progress.period ?? "MONTHLY",
            targetAmount: parseNumber(progress.targetAmount),
            spentAmount: parseNumber(progress.spentAmount),
            remainingAmount: parseNumber(progress.remainingAmount),
            progressPercent: parseNumber(progress.progressPercent),
            daysLeft: progress.daysLeft ?? 0,
            health: progress.health ?? "ON_TRACK",
            alertLevel: progress.alertLevel ?? "NONE",
            startDate: progress.startDate ?? null,
            endDate: progress.endDate ?? null,
            warningThreshold: progress.warningThreshold ?? 80,
            overBudgetThreshold: progress.overBudgetThreshold ?? 100,
          });
      }
    });

      setGoals(combined);

      if (activeGoals.length > 0) {
        setCategories((prev) => {
          const byId = new Map(prev.map((category) => [category.id, category]));
          activeGoals.forEach((goal) => {
            if (goal.categoryId && goal.categoryName) {
              byId.set(goal.categoryId, {
                id: goal.categoryId,
                name: goal.categoryName,
              });
            }
          });
          return Array.from(byId.values()).sort((a, b) =>
            a.name.localeCompare(b.name),
          );
        });
      }
    } catch (error) {
      console.error("Failed to fetch goals", error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        navigate("/login", {
          state: { message: "Session expired. Please log in again." },
        });
      } else {
        setFetchError("Unable to load goals right now. Please try again shortly.");
      }
      setGoals([]);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      fetchGoals();
    }
  }, [authLoading, isAuthenticated, fetchGoals]);

  const totals = useMemo(() => {
    if (!goals.length) {
      return {
        totalTarget: 0,
        totalSpent: 0,
        totalRemaining: 0,
        overallProgress: 0,
      };
    }

    const totalTarget = goals.reduce(
      (sum, goal) => sum + parseNumber(goal.targetAmount),
      0,
    );
    const totalSpent = goals.reduce(
      (sum, goal) => sum + parseNumber(goal.spentAmount),
      0,
    );
    const totalRemaining = Math.max(totalTarget - totalSpent, 0);
    const overallProgress = totalTarget > 0 ? (totalSpent / totalTarget) * 100 : 0;

    return {
      totalTarget,
      totalSpent,
      totalRemaining,
      overallProgress,
    };
  }, [goals]);

  const handleOpenModal = () => {
    setErrors({});
    setGoalNameEdited(false);
    setForm(createEmptyFormState(categories));
    setModalOpen(true);
  };

  const handleCloseModal = () => {
    setModalOpen(false);
    setSubmitting(false);
    setGoalNameEdited(false);
  };

  const handleInputChange = (field) => (event) => {
    const { type, checked, value } = event.target;
    const nextValue = type === "checkbox" ? checked : value;
    if (field === "goalName") {
      setGoalNameEdited(true);
    }
    setForm((prev) => {
      const nextState = { ...prev, [field]: nextValue };
      if (field === "categoryId" && !goalNameEdited) {
        const selected = categories.find((category) => String(category.id) === String(nextValue));
        if (selected) {
          nextState.goalName = `${selected.name} Goal`;
        }
      }
      return nextState;
    });
    if (errors[field]) {
      setErrors((prev) => {
        const { [field]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  const validateForm = () => {
    const validationErrors = {};

    const trimmedGoalName = form.goalName ? form.goalName.trim() : "";
    if (!trimmedGoalName) {
      validationErrors.goalName = "Enter a name to identify this goal";
    } else if (trimmedGoalName.length > 128) {
      validationErrors.goalName = "Goal name must be 128 characters or fewer";
    }

    if (!form.categoryId) {
      validationErrors.categoryId = "Select a spending category";
    }

    if (!form.period) {
      validationErrors.period = "Choose how often this goal should apply";
    }

    const amount = parseNumber(form.targetAmount);
    if (!form.targetAmount || Number.isNaN(amount) || amount <= 0) {
      validationErrors.targetAmount = "Enter a target amount greater than 0";
    }

    return validationErrors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationErrors = validateForm();

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    const payload = {
      categoryId: Number(form.categoryId),
      period: form.period,
      targetAmount: parseNumber(form.targetAmount),
      goalName: form.goalName.trim(),
      confirmDuplicate: form.confirmDuplicate,
      startNextPeriod: form.startNextPeriod,
    };

    try {
      setSubmitting(true);
      setErrors({});
      await goalsService.createGoal(payload);
      await fetchGoals();
      setModalOpen(false);
      setForm(createEmptyFormState(categories));
      setGoalNameEdited(false);
    } catch (error) {
      console.error("Failed to create goal", error);
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Failed to save goal. Please try again.";
      setErrors({ submit: message });
    } finally {
      setSubmitting(false);
    }
  };

  const safeOverallProgress = Math.min(Math.max(totals.overallProgress, 0), 100);

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex flex-col gap-2 mb-10">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400">Overview</span>
        <div className="flex items-center justify-between gap-4">
          <h1 className="text-3xl font-semibold text-gray-900 dark:text-white">Spending Goals</h1>
          <button
            type="button"
            onClick={handleOpenModal}
            className="flex items-center gap-2 rounded-full bg-gray-900 dark:bg-gray-700 px-6 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-gray-600"
          >
            <FaPlus /> Add Goal
          </button>
        </div>
        <p className="text-gray-500 dark:text-gray-400">
          Track how each category is pacing against the budget you set.
        </p>
      </div>

      {fetchError && (
        <div className="mb-6 rounded-3xl border border-rose-200 bg-rose-50 px-6 py-4 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-900/10 dark:text-rose-200">
          {fetchError}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 mb-10">
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Current Spending</p>
          <p className="mt-3 text-2xl font-semibold text-gray-900 dark:text-white">
            {formatCurrency(totals.totalSpent)}
          </p>
          <p className="mt-2 text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
            Across {goals.length} active {goals.length === 1 ? "goal" : "goals"}
          </p>
        </div>
        
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Target Budget</p>
          <p className="mt-3 text-2xl font-semibold text-gray-900 dark:text-white">
            {formatCurrency(totals.totalTarget)}
          </p>
          <p className="mt-2 text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
            Remaining {formatCurrency(totals.totalRemaining)}
          </p>
        </div>
      </div>

      <div className="bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-3xl shadow-sm p-6 mb-10 transition-colors duration-200">
        <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-4">Progress</p>
        <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3 transition-colors duration-200">
          <div
            className="h-3 rounded-full bg-gray-900 dark:bg-indigo-500 transition-all duration-300"
            style={{ width: `${safeOverallProgress}%` }}
          ></div>
        </div>
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-2 transition-colors duration-200">
          {safeOverallProgress.toFixed(1)}% of total targets achieved
        </p>
      </div>

      <div className="bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-3xl shadow-sm p-6 sm:p-10 transition-colors duration-200">
        <div className="mb-8">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Current Goals</p>
        </div>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">Loading goals...</p>
        ) : goals.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No goals yet. Start by creating one with the button above.
          </p>
        ) : (
          <ul className="divide-y divide-gray-100 dark:divide-gray-700">
            {goals.map((goal) => {
              const progress = Math.min(Math.max(goal.progressPercent ?? 0, 0), 120);
              const healthLabel =
                HEALTH_LABELS[goal.health] ?? HEALTH_LABELS.ON_TRACK;
              const healthStyle =
                HEALTH_STYLES[goal.health] ?? HEALTH_STYLES.ON_TRACK;
              const alertLabel = ALERT_LABELS[goal.alertLevel];
              const alertStyle = ALERT_STYLES[goal.alertLevel];

              return (
                <li
                  key={goal.goalId}
                  className="flex flex-col gap-4 py-6 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex-1">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-baseline sm:justify-between">
                      <div>
                        <p className="text-lg font-semibold text-gray-900 dark:text-white">
                          {goal.goalName}
                        </p>
                        <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                          {goal.categoryName} • {formatPeriodLabel(goal.period)} • {formatDate(goal.startDate)} — {formatDate(goal.endDate)}
                        </p>
                      </div>
                      <span className="text-sm font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                        {formatCurrency(goal.targetAmount)}
                      </span>
                    </div>

                    <div className="mt-4 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                      <div
                        className="h-2 rounded-full bg-gray-900 dark:bg-indigo-500 transition-all duration-300"
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      ></div>
                    </div>

                    <div className="mt-3 grid gap-3 text-sm text-gray-500 dark:text-gray-400 sm:grid-cols-3">
                      <div>
                        <p className="text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
                          Spent
                        </p>
                        <p className="mt-1 font-medium text-gray-600 dark:text-gray-300">
                          {formatCurrency(goal.spentAmount)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
                          Remaining
                        </p>
                        <p className="mt-1 font-medium text-gray-600 dark:text-gray-300">
                          {formatCurrency(goal.remainingAmount)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
                          Progress
                        </p>
                        <p className="mt-1 font-medium text-gray-600 dark:text-gray-300">
                          {parseNumber(goal.progressPercent).toFixed(1)}%
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col items-start gap-3 sm:items-end">
                    <span
                      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.25em] ${healthStyle}`}
                    >
                      {healthLabel}
                    </span>
                    <p className="text-xs uppercase tracking-[0.25em] text-gray-400 dark:text-gray-500">
                      {goal.daysLeft} {goal.daysLeft === 1 ? "day" : "days"} left
                    </p>
                    {alertLabel && (
                      <div className={`flex items-center gap-2 text-sm ${alertStyle}`}>
                        <FaExclamationTriangle className="text-xs" />
                        <span>{alertLabel}</span>
                      </div>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-xl bg-white dark:bg-gray-800 rounded-3xl shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700 px-6 sm:px-10 py-6 rounded-t-3xl flex items-center justify-between">
              <h2 className="text-2xl font-semibold text-gray-900 dark:text-white">
                Add Spending Goal
              </h2>
              <button
                type="button"
                onClick={handleCloseModal}
                className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              >
                <FaTimes className="text-2xl" />
              </button>
            </div>

            <div className="px-6 sm:px-10 py-8">
              <form className="space-y-8" onSubmit={handleSubmit} noValidate>
                {errors.submit && (
                  <div className="p-3 bg-rose-100 dark:bg-rose-900/30 border border-rose-300 dark:border-rose-700 text-rose-700 dark:text-rose-200 rounded-md">
                    {errors.submit}
                  </div>
                )}

                <div>
                  <label className={LABEL_CLASSES}>Goal Name</label>
                  <div className={BORDER_SECTION_CLASSES}>
                    <input
                      type="text"
                      value={form.goalName}
                      onChange={handleInputChange("goalName")}
                      className={INPUT_BASE_CLASSES}
                      placeholder="e.g., Groceries Monthly Budget"
                    />
                  </div>
                  {errors.goalName && (
                    <p className="mt-2 text-xs text-rose-500">{errors.goalName}</p>
                  )}
                </div>

                <div>
                  <label className={LABEL_CLASSES}>Category</label>
                  <div className={`${BORDER_SECTION_CLASSES} relative`}>
                    <select
                      value={form.categoryId}
                      onChange={handleInputChange("categoryId")}
                      className={`${INPUT_BASE_CLASSES} appearance-none pr-10`}
                    >
                      <option value="" disabled>
                        Select category
                      </option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                    <FaChevronDown className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-xs text-gray-400 dark:text-gray-500" />
                  </div>
                  {errors.categoryId && (
                    <p className="mt-2 text-xs text-rose-500">{errors.categoryId}</p>
                  )}
                </div>

                <div>
                  <label className={LABEL_CLASSES}>Target Amount</label>
                  <div className={BORDER_SECTION_CLASSES}>
                    <input
                      type="number"
                      value={form.targetAmount}
                      onChange={handleInputChange("targetAmount")}
                      className={`${INPUT_BASE_CLASSES} appearance-none`}
                      placeholder="0.00"
                      min="0"
                      step="0.01"
                    />
                  </div>
                  {errors.targetAmount && (
                    <p className="mt-2 text-xs text-rose-500">{errors.targetAmount}</p>
                  )}
                </div>

                <div>
                  <label className={LABEL_CLASSES}>Goal Period</label>
                  <div className={`${BORDER_SECTION_CLASSES} relative`}>
                    <select
                      value={form.period}
                      onChange={handleInputChange("period")}
                      className={`${INPUT_BASE_CLASSES} appearance-none pr-10`}
                    >
                      {PERIOD_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <FaChevronDown className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-xs text-gray-400 dark:text-gray-500" />
                  </div>
                  {errors.period && (
                    <p className="mt-2 text-xs text-rose-500">{errors.period}</p>
                  )}
                </div>

                <div className="grid gap-6 sm:grid-cols-2">
                  <label className="flex items-start gap-3 text-sm text-gray-600 dark:text-gray-300">
                    <input
                      type="checkbox"
                      checked={form.startNextPeriod}
                      onChange={handleInputChange("startNextPeriod")}
                      className="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                    />
                    <span>
                      Start from next {form.period.toLowerCase()} instead of the current one.
                    </span>
                  </label>
                  <label className="flex items-start gap-3 text-sm text-gray-600 dark:text-gray-300">
                    <input
                      type="checkbox"
                      checked={form.confirmDuplicate}
                      onChange={handleInputChange("confirmDuplicate")}
                      className="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                    />
                    <span>
                      Replace any active goal for the same category and period.
                    </span>
                  </label>
                </div>

                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-end">
                  <button
                    type="button"
                    onClick={handleCloseModal}
                    className="w-full sm:w-auto rounded-full border border-gray-200 dark:border-gray-600 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-600 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full sm:w-auto rounded-full bg-gray-900 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 disabled:cursor-not-allowed disabled:bg-gray-600"
                  >
                    {submitting ? "Saving..." : "Save Goal"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
