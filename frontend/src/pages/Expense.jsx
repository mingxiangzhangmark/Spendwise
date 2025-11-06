import React, { useEffect, useMemo, useState } from "react";
import { FaChevronDown, FaEdit, FaTrash, FaExclamationTriangle, FaPlus, FaTimes, FaSearch, FaFilter } from "react-icons/fa";
import { expenseRecordService } from "../services/api";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const CATEGORY_OPTIONS = {
  expense: [
    "Food",           // ID: 1
    "Transport",      // ID: 2
    "Entertainment",  // ID: 3
    "Shopping",       // ID: 4
    "Utilities",      // ID: 5
  ],
};

// Backend categories with their database IDs (from PostgreSQL query)
const getCategoryId = (categoryName) => {
  const categoryMap = {
    "Food": 1,
    "Transport": 2,
    "Entertainment": 3,
    "Shopping": 4,
    "Utilities": 5,
  };
  return categoryMap[categoryName] || 1; // Default to Food if not found
};

const INITIAL_TRANSACTIONS = [];

const CURRENCY_OPTIONS = ["USD", "AUD", "EUR", "GBP"];

const FREQUENCY_OPTIONS = [
  { value: "DAILY", label: "Daily" },
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
];

const formatCurrency = (value, currency) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const formatDate = (value) =>
  new Date(value).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

const createEmptyFormState = () => ({
  amount: "",
  currency: "USD",
  mode: "expense",
  category: CATEGORY_OPTIONS.expense[0],
  description: "",
  isRecurring: false,
  frequency: "MONTHLY",
});

export default function Expense() {
  const { isAuthenticated, loading: authLoading, user } = useAuth();
  const navigate = useNavigate();
  
  // Debug: Log auth status
  useEffect(() => {
    console.log("Auth status:", { isAuthenticated, authLoading, user });
  }, [isAuthenticated, authLoading, user]);
  
  const [transactions, setTransactions] = useState(INITIAL_TRANSACTIONS);
  const [form, setForm] = useState(() => createEmptyFormState());
  const [errors, setErrors] = useState({});
  const [editingId, setEditingId] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState({ show: false, transaction: null });
  const [dropdownOpen, setDropdownOpen] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [categoryMap, setCategoryMap] = useState({
    // Hardcoded fallback - matches database
    "Food": 1,
    "Transport": 2,
    "Entertainment": 3,
    "Shopping": 4,
    "Utilities": 5,
  }); // Store actual category IDs from backend

  // Search and filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [selectedCurrency, setSelectedCurrency] = useState("all");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [amountMin, setAmountMin] = useState("");
  const [amountMax, setAmountMax] = useState("");
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [showOnlyRecurring, setShowOnlyRecurring] = useState(false);

  const { mode } = form;

  // Check authentication
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate('/login', { state: { message: 'Please log in to access your expenses' } });
    }
  }, [isAuthenticated, authLoading, navigate]);

  // Fetch transactions from backend
  useEffect(() => {
    // Only fetch if authenticated
    if (!isAuthenticated || authLoading) {
      console.log("Skipping fetch - authenticated:", isAuthenticated, "loading:", authLoading);
      return;
    }

    const fetchTransactions = async () => {
      try {
        setLoading(true);
        console.log("Fetching transactions from backend...");
        const response = await expenseRecordService.getAllRecords();
        console.log("Fetch response:", response);
        
        // Build category ID map from actual backend data
        const backendCategoryMap = {};
        response.forEach((record) => {
          if (record.category) {
            backendCategoryMap[record.category.name] = record.category.id;
          }
        });
        
        // If no records exist, the map will be empty - log this
        if (Object.keys(backendCategoryMap).length === 0) {
          console.warn("No existing records found. Category IDs cannot be determined from backend data.");
          console.warn("Will use fallback category IDs. This may cause errors if database IDs don't match.");
        }
        
        // Store the category map for creating new records
        setCategoryMap(backendCategoryMap);
        console.log("Backend category IDs:", backendCategoryMap);
        console.log("Total records fetched:", response.length);
        
        // Transform backend data to frontend format
        // Backend stores all as positive amounts for expenses only
        const transformedData = response.map((record) => {
          const categoryName = record.category?.name || "Food";
          const backendAmount = parseFloat(record.amount);
          
          // // Determine mode based on amount sign (COMMENTED OUT - INCOME DISABLED)
          // const isIncome = backendAmount < 0;
          
          return {
            id: record.expenseId,
            amount: Math.abs(backendAmount),
            currency: record.currency,
            mode: 'expense',
            category: categoryName,
            description: record.description || '',
            date: record.expenseDate ? new Date(record.expenseDate).toISOString() : new Date().toISOString(),
            isRecurring: record.isRecurring || false,
            frequency: record.frequency || "MONTHLY",
          };
        });
        
        setTransactions(transformedData);
        setFetchError(null);
      } catch (error) {
        console.error("Failed to fetch transactions:", error);
        if (error.response?.status === 500) {
          setFetchError("Server error. Please try logging out and logging back in.");
        } else if (error.response?.status === 401 || error.response?.status === 403) {
          setFetchError("Authentication error. Please log in again.");
          navigate('/login', { state: { message: 'Session expired. Please log in again.' } });
        } else {
          setFetchError("Failed to load transactions. Please try again later.");
        }
        // Keep empty array on error
        setTransactions([]);
      } finally {
        setLoading(false);
      }
    };

    fetchTransactions();
  }, [isAuthenticated, authLoading, navigate]);

  useEffect(() => {
    const availableCategories = CATEGORY_OPTIONS[mode] ?? [];
    setForm((prev) => {
      const nextCategory = availableCategories.includes(prev.category)
        ? prev.category
        : availableCategories[0] || "";

      if (nextCategory === prev.category) {
        return prev;
      }

      return { ...prev, category: nextCategory };
    });
  }, [mode]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest('.relative')) {
        setDropdownOpen(null);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const totalsByCurrency = useMemo(
    () =>
      transactions.reduce((acc, item) => {
        const currencyKey = item.currency || "USD";
        if (!acc[currencyKey]) {
          acc[currencyKey] = { expense: 0 };
        }

        const numericAmount = Number(item.amount) || 0;
        acc[currencyKey].expense += numericAmount;

        return acc;
      }, {}),
    [transactions]
  );

  const currencyTotals = totalsByCurrency[form.currency] ?? {
    expense: 0,
  };

  const clearAllFilters = () => {
    setSearchTerm("");
    setSelectedCategory("all");
    setSelectedCurrency("all");
    setDateFrom("");
    setDateTo("");
    setAmountMin("");
    setAmountMax("");
    setShowOnlyRecurring(false);
  };

  const hasActiveFilters = searchTerm || selectedCategory !== "all" || selectedCurrency !== "all" ||
    dateFrom || dateTo || amountMin || amountMax || showOnlyRecurring;

  const visibleTransactions = useMemo(() => {
    let filtered = transactions;

    // Apply recurring filter
    if (showOnlyRecurring) {
      filtered = filtered.filter((transaction) => transaction.isRecurring);
    }

    // Apply search term
    if (searchTerm.trim()) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter((transaction) =>
        transaction.description.toLowerCase().includes(searchLower) ||
        transaction.category.toLowerCase().includes(searchLower) ||
        (transaction.isRecurring && transaction.frequency?.toLowerCase().includes(searchLower))
      );
    }

    // Apply category filter
    if (selectedCategory !== "all") {
      filtered = filtered.filter((transaction) => transaction.category === selectedCategory);
    }

    // Apply currency filter
    if (selectedCurrency !== "all") {
      filtered = filtered.filter((transaction) => transaction.currency === selectedCurrency);
    }

    // Apply date range filter
    if (dateFrom) {
      const fromDate = new Date(dateFrom);
      filtered = filtered.filter((transaction) => new Date(transaction.date) >= fromDate);
    }
    if (dateTo) {
      const toDate = new Date(dateTo);
      toDate.setHours(23, 59, 59, 999); // End of day
      filtered = filtered.filter((transaction) => new Date(transaction.date) <= toDate);
    }

    // Apply amount range filter
    if (amountMin) {
      const minAmount = parseFloat(amountMin);
      filtered = filtered.filter((transaction) => transaction.amount >= minAmount);
    }
    if (amountMax) {
      const maxAmount = parseFloat(amountMax);
      filtered = filtered.filter((transaction) => transaction.amount <= maxAmount);
    }

    return filtered;
  }, [transactions, showOnlyRecurring, searchTerm, selectedCategory, selectedCurrency, dateFrom, dateTo, amountMin, amountMax]);

  const handleChange = (field) => (event) => {
    const value = event.target.value;
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));

    setErrors((prev) => {
      if (!prev[field]) {
        return prev;
      }

      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const nextErrors = {};
    const numericAmount = parseFloat(form.amount);

    if (Number.isNaN(numericAmount) || numericAmount <= 0) {
      nextErrors.amount = "Enter an amount greater than 0.";
    }

    if (!form.category) {
      nextErrors.category = "Choose a category.";
    }

    if (!form.description.trim()) {
      nextErrors.description = "Add a brief description.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    const baseTransaction = {
      amount: Number(numericAmount.toFixed(2)),
      currency: form.currency,
      mode: form.mode,
      category: form.category,
      description: form.description.trim(),
    };

    try {
      // Get the category ID based on category name
      // Use dynamic category map if available, otherwise fall back to hardcoded IDs
      let categoryId;
      if (categoryMap && categoryMap[form.category]) {
        categoryId = categoryMap[form.category];
        console.log(`Using dynamic category ID for ${form.category}: ${categoryId}`);
      } else {
        categoryId = getCategoryId(form.category);
        console.warn(`Using fallback category ID for ${form.category}: ${categoryId}. This might cause errors if database IDs don't match.`);
      }

      // For backend: all amounts are positive for expenses
      const backendAmount = Math.abs(baseTransaction.amount);
      
      // // For backend: income is stored as negative amount, expense as positive (COMMENTED OUT - INCOME DISABLED)
      // const backendAmount = form.mode === 'income' 
      //   ? -Math.abs(baseTransaction.amount) 
      //   : Math.abs(baseTransaction.amount);

      if (editingId) {
        // Update existing transaction via API
        const recordData = {
          amount: backendAmount,
          currency: baseTransaction.currency,
          description: baseTransaction.description,
          expenseDate: new Date().toISOString().split('T')[0], // YYYY-MM-DD format
        };

        await expenseRecordService.updateRecord(editingId, recordData, form.isRecurring ? form.frequency : null);
        
        // Update local state
        setTransactions((prev) =>
          prev.map((transaction) =>
            transaction.id === editingId
              ? {
                  ...transaction,
                  ...baseTransaction,
                  date: new Date().toISOString(),
                  isRecurring: form.isRecurring,
                  frequency: form.frequency,
                }
              : transaction
          )
        );
      } else {
        // Create new transaction via API
        const recordData = {
          category: {
            categoryId: Number(categoryId)
          },
          amount: Number(backendAmount),
          currency: baseTransaction.currency,
          description: baseTransaction.description || "",
          expenseDate: new Date().toISOString().split('T')[0],
          isRecurring: form.isRecurring || false,
          paymentMethod: "Credit Card",
          notes: "",
        };

        console.log("Creating expense record with data:", JSON.stringify(recordData, null, 2));
        const response = await expenseRecordService.createRecord(recordData, form.isRecurring ? form.frequency : null);
        console.log("Created expense record response:", response);
        
        // Add to local state
        setTransactions((prev) => [
          {
            id: response.expenseId || Date.now(),
            date: new Date().toISOString(),
            ...baseTransaction,
            isRecurring: form.isRecurring,
            frequency: form.frequency,
          },
          ...prev,
        ]);
      }

      // Reset form and close modal
      setForm((prev) => ({
        ...prev,
        amount: "",
        description: "",
      }));
      setErrors({});
      setEditingId(null);
      setModalOpen(false);
    } catch (error) {
      console.error("Failed to save transaction:", error);
      setErrors({ submit: "Failed to save transaction. Please try again." });
    }
  };

  const startEditing = (transaction) => {
    setForm({
      amount: String(transaction.amount),
      currency: transaction.currency,
      mode: transaction.mode,
      category: transaction.category,
      description: transaction.description,
      isRecurring: transaction.isRecurring || false,
      frequency: transaction.frequency || "MONTHLY",
    });
    setErrors({});
    setEditingId(transaction.id);
    setModalOpen(true);
  };

  const cancelEditing = () => {
    setForm(createEmptyFormState());
    setErrors({});
    setEditingId(null);
    setModalOpen(false);
  };

  const openDeleteConfirm = (transaction) => {
    setConfirmDelete({ show: true, transaction });
    setDropdownOpen(null);
  };

  const closeDeleteConfirm = () => {
    setConfirmDelete({ show: false, transaction: null });
  };

  const toggleDropdown = (transactionId) => {
    setDropdownOpen(dropdownOpen === transactionId ? null : transactionId);
  };

  const handleEdit = (transaction) => {
    startEditing(transaction);
    setDropdownOpen(null);
  };

  const confirmDeleteTransaction = async () => {
    if (!confirmDelete.transaction) {
      return;
    }

    try {
      // Call backend API to delete
      await expenseRecordService.deleteRecord(confirmDelete.transaction.id);
      
      // Update local state
      setTransactions((prev) =>
        prev.filter((transaction) => transaction.id !== confirmDelete.transaction.id)
      );

      if (editingId === confirmDelete.transaction.id) {
        cancelEditing();
      }

      closeDeleteConfirm();
    } catch (error) {
      console.error("Failed to delete transaction:", error);
      // Optionally show error to user
      alert("Failed to delete transaction. Please try again.");
    }
  };

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex flex-col gap-2 mb-10">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Overview</span>
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-semibold text-gray-900 dark:text-white">Transactions</h1>
          <button
            onClick={() => {
              setForm(createEmptyFormState());
              setEditingId(null);
              setErrors({});
              setModalOpen(true);
            }}
            className="flex items-center gap-2 rounded-full bg-gray-900 dark:bg-gray-700 px-6 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-gray-600"
          >
            <FaPlus /> Add Transaction
          </button>
        </div>
        <p className="text-gray-500 dark:text-gray-400">
          Log your spending and recurring transactions to keep an eye on your cash flow.
        </p>
      </div>

      {loading && (
        <div className="text-center py-10">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-800 dark:border-gray-300 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading transactions...</p>
        </div>
      )}

      {fetchError && (
        <div className="mb-6 p-4 bg-yellow-100 dark:bg-yellow-900 border border-yellow-400 dark:border-yellow-600 text-yellow-700 dark:text-yellow-200 rounded-lg">
          {fetchError}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-1 mb-10">
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Total Expense</p>
          <p className="mt-3 text-2xl font-semibold text-rose-500">
            {formatCurrency(currencyTotals.expense, form.currency)}
          </p>
        </div>
      </div>

      <div className="bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-3xl shadow-sm p-6 sm:p-10 transition-colors duration-200">
        <div className="mb-6 space-y-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Recent Activity</h2>
            <div className="flex gap-2">
              <button
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                  showAdvancedFilters
                    ? "bg-blue-600 text-white"
                    : "bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                }`}
              >
                <FaFilter /> Filters
              </button>
              {hasActiveFilters && (
                <button
                  onClick={clearAllFilters}
                  className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                >
                  Clear All
                </button>
              )}
            </div>
          </div>

          {/* Search Bar */}
          <div className="relative">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500" />
            <input
              type="text"
              placeholder="Search transactions by description, category, or frequency..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Recurring Filter Checkbox */}
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={showOnlyRecurring}
                onChange={(e) => setShowOnlyRecurring(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 dark:border-gray-600 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Show only recurring transactions</span>
            </label>
          </div>

          {/* Advanced Filters */}
          {showAdvancedFilters && (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-xl">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Category
                </label>
                <select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="all">All Categories</option>
                  {CATEGORY_OPTIONS.expense.map((category) => (
                    <option key={category} value={category}>
                      {category}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Currency
                </label>
                <select
                  value={selectedCurrency}
                  onChange={(e) => setSelectedCurrency(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="all">All Currencies</option>
                  {CURRENCY_OPTIONS.map((currency) => (
                    <option key={currency} value={currency}>
                      {currency}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Date From
                </label>
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Date To
                </label>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Min Amount
                </label>
                <input
                  type="number"
                  placeholder="0.00"
                  value={amountMin}
                  onChange={(e) => setAmountMin(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  min="0"
                  step="0.01"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 mb-2">
                  Max Amount
                </label>
                <input
                  type="number"
                  placeholder="0.00"
                  value={amountMax}
                  onChange={(e) => setAmountMax(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  min="0"
                  step="0.01"
                />
              </div>
            </div>
          )}
        </div>

        {visibleTransactions.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">No transactions logged yet.</p>
        ) : (
          <ul className="divide-y divide-gray-100 dark:divide-gray-700">
            {visibleTransactions.map((transaction) => {
              const amountColor = transaction.mode === "expense" ? "text-rose-500" : "text-emerald-500";
              return (
                <li
                  key={transaction.id}
                  className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between relative"
                >
                  <div className="flex-1">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-baseline sm:justify-between">
                      <div>
                        <p className="text-lg font-semibold text-gray-900 dark:text-white">
                          {transaction.category}
                        </p>
                        <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                          {formatDate(transaction.date)}
                          {transaction.isRecurring && transaction.frequency && (
                            <span className="ml-2 text-emerald-500">
                              • Recurring • {transaction.frequency}
                            </span>
                          )}
                        </p>
                      </div>
                      <span className={`text-sm font-semibold uppercase tracking-[0.3em] ${amountColor}`}>
                        {transaction.mode === "expense" ? "-" : "+"}
                        {formatCurrency(transaction.amount, transaction.currency)}
                      </span>
                    </div>
                    {transaction.description && (
                      <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">{transaction.description}</p>
                    )}
                  </div>

                  <div className="flex flex-col items-start gap-3 sm:items-end">
                    <div className="relative">
                      <button
                        type="button"
                        onClick={() => toggleDropdown(transaction.id)}
                        className="flex items-center gap-2 rounded-full border border-gray-200 dark:border-gray-600 px-4 py-2 text-xs font-semibold uppercase tracking-[0.25em] text-gray-600 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        Manage
                        <FaChevronDown className="text-gray-400 dark:text-gray-500 text-sm" />
                      </button>
                      {dropdownOpen === transaction.id && (
                        <div className="absolute right-0 top-full mt-2 z-10 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-2xl shadow-lg min-w-[140px]">
                          <button
                            type="button"
                            onClick={() => handleEdit(transaction)}
                            className="w-full flex items-center gap-2 px-4 py-3 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 first:rounded-t-2xl"
                          >
                            <FaEdit className="text-gray-500 dark:text-gray-400" /> Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => openDeleteConfirm(transaction)}
                            className="w-full flex items-center gap-2 px-4 py-3 text-sm text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20 last:rounded-b-2xl border-t border-gray-100 dark:border-gray-700"
                          >
                            <FaTrash className="text-rose-500" /> Delete
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {/* Add/Edit Transaction Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white dark:bg-gray-800 rounded-3xl shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700 px-6 sm:px-10 py-6 rounded-t-3xl flex items-center justify-between">
              <h2 className="text-2xl font-semibold text-gray-900 dark:text-white">
                {editingId ? "Update Transaction" : "Add Transaction"}
              </h2>
              <button
                onClick={cancelEditing}
                className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              >
                <FaTimes className="text-2xl" />
              </button>
            </div>

            <div className="px-6 sm:px-10 py-8">
              <form className="space-y-8" onSubmit={handleSubmit} noValidate>
                {errors.submit && (
                  <div className="p-3 bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 rounded-md">
                    {errors.submit}
                  </div>
                )}

                <div>
                  <label className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                    Amount
                  </label>
                  <div className="mt-3 border-b border-gray-200 dark:border-gray-600 pb-5">
                    <div className="flex items-end justify-between">
                      <div className="flex items-baseline gap-2">
                        <span className="text-4xl font-semibold text-gray-900 dark:text-white">$</span>
                        <input
                          type="number"
                          inputMode="decimal"
                          step="0.01"
                          min="0"
                          placeholder="0.00"
                          value={form.amount}
                          onChange={handleChange("amount")}
                          className="w-40 bg-transparent text-4xl font-semibold text-gray-900 dark:text-white placeholder:text-gray-300 dark:placeholder:text-gray-600 focus:outline-none"
                        />
                      </div>
                      <div className="relative">
                        <select
                          value={form.currency}
                          onChange={handleChange("currency")}
                          className="appearance-none bg-transparent text-sm font-medium tracking-[0.3em] uppercase text-gray-500 dark:text-gray-400 pr-6 focus:outline-none"
                          aria-label="Select currency"
                        >
                          {CURRENCY_OPTIONS.map((currency) => (
                            <option key={currency} value={currency}>
                              {currency}
                            </option>
                          ))}
                        </select>
                        <FaChevronDown className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-xs text-gray-400 dark:text-gray-500" />
                      </div>
                    </div>
                    {errors.amount && (
                      <p className="mt-2 text-xs text-red-500">{errors.amount}</p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                    Category
                  </label>
                  <div className="relative mt-3 border-b border-gray-200 dark:border-gray-600">
                    <select
                      value={form.category}
                      onChange={handleChange("category")}
                      className="w-full appearance-none bg-transparent py-3 text-lg font-medium text-gray-900 dark:text-white focus:outline-none"
                      aria-label="Select category"
                    >
                      {CATEGORY_OPTIONS[form.mode].map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                    <FaChevronDown className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500" />
                  </div>
                  {errors.category && (
                    <p className="mt-2 text-xs text-red-500">{errors.category}</p>
                  )}
                </div>

                <div>
                  <label className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                    Descriptions
                  </label>
                  <div className="mt-3 border-b border-gray-200 dark:border-gray-600">
                    <textarea
                      rows={2}
                      value={form.description}
                      onChange={handleChange("description")}
                      placeholder="Enter description"
                      className="w-full resize-none bg-transparent py-3 text-base text-gray-900 dark:text-white placeholder:text-gray-400 dark:placeholder:text-gray-600 focus:outline-none"
                    />
                  </div>
                  {errors.description && (
                    <p className="mt-2 text-xs text-red-500">{errors.description}</p>
                  )}
                </div>

                {/* Recurring Checkbox */}
                <div className="flex items-center">
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.isRecurring}
                      onChange={(e) => setForm(prev => ({ ...prev, isRecurring: e.target.checked }))}
                      className="h-4 w-4 rounded border-gray-300 dark:border-gray-600 text-gray-600 focus:ring-gray-500"
                    />
                    <span className="text-sm font-semibold text-gray-900 dark:text-white">Recurring Transaction</span>
                  </label>
                </div>

                {/* Frequency Selection - only show when recurring is checked */}
                {form.isRecurring && (
                  <div>
                    <label className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">
                      Frequency
                    </label>
                    <div className="relative mt-3 border-b border-gray-200 dark:border-gray-600">
                      <select
                        value={form.frequency}
                        onChange={handleChange("frequency")}
                        className="w-full appearance-none bg-transparent py-3 text-lg font-medium text-gray-900 dark:text-white focus:outline-none"
                        aria-label="Select frequency"
                      >
                        {FREQUENCY_OPTIONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      <FaChevronDown className="pointer-events-none absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500" />
                    </div>
                  </div>
                )}

                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-end pt-4">
                  <button
                    type="button"
                    onClick={cancelEditing}
                    className="w-full rounded-full border border-gray-200 dark:border-gray-600 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-600 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700 sm:w-auto sm:min-w-[140px]"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="w-full rounded-full bg-gray-900 dark:bg-gray-700 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-gray-600 sm:w-auto sm:min-w-[140px]"
                  >
                    {editingId ? "Save Changes" : "Save"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {confirmDelete.show && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-xl">
            <div className="flex items-center gap-3 text-rose-500">
              <FaExclamationTriangle />
              <p className="text-xs font-semibold uppercase tracking-[0.3em]">Delete Transaction</p>
            </div>
            <h3 className="mt-4 text-xl font-semibold text-gray-900">Are you sure?</h3>
            <p className="mt-2 text-sm text-gray-500">
              This will permanently remove "{confirmDelete.transaction?.description}" from
              your history. You can’t undo this action.
            </p>
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={closeDeleteConfirm}
                className="w-full rounded-full border border-gray-200 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-600 transition-colors hover:bg-gray-100 sm:w-auto sm:min-w-[120px]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmDeleteTransaction}
                className="w-full rounded-full bg-rose-500 px-8 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-rose-600 sm:w-auto sm:min-w-[120px]"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
