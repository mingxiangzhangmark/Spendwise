import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  FaUser,
  FaBell,
  FaEye,
  FaMoon,
  FaDollarSign,
  FaChevronRight,
  FaCheckCircle,
  FaExclamationCircle,
  FaExclamationTriangle,
  FaInfoCircle,
} from "react-icons/fa";
import { useFont } from "../context/FontContext";
import { useDarkMode } from "../context/DarkModeContext";
import { authService } from "../services/api";

export default function Settings() {
  // const [notifications, setNotifications] = useState(true);
  const { dyslexiaFont, setDyslexiaFont } = useFont();
  const { darkMode, toggleDarkMode } = useDarkMode();
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ show: false, message: "", type: "info" });
  const navigate = useNavigate();

  // Debug: Log dark mode state
  useEffect(() => {
    console.log('⚙️ Settings: Dark mode is', darkMode);
  }, [darkMode]);

  const TOAST_ICON = {
    success: FaCheckCircle,
    error: FaExclamationCircle,
    warning: FaExclamationTriangle,
    info: FaInfoCircle,
  };

  const ToastIcon = toast.type ? TOAST_ICON[toast.type] : TOAST_ICON.info;

  const preferenceToggles = [
    // {
    //   id: "notifications",
    //   label: "Notifications",
    //   description: "Get reminders about bills, goals, and budget alerts.",
    //   icon: FaBell,
    //   value: notifications,
    //   onToggle: () => setNotifications((prev) => !prev),
    // },
    {
      id: "dyslexiaFont",
      label: "Dyslexia-friendly Font",
      description: "Use typography that's easier to scan quickly.",
      icon: FaEye,
      value: dyslexiaFont,
      onToggle: () => setDyslexiaFont((prev) => !prev),
    },
    {
      id: "darkTheme",
      label: "Dark Theme",
      description: "Reduce glare and make SpendWise easier on the eyes.",
      icon: FaMoon,
      value: darkMode,
      onToggle: toggleDarkMode,
    },
  ];

  const showToast = (message, type = "info") => {
    setToast({ show: true, message, type });
    setTimeout(() => {
      setToast((prev) => ({ ...prev, show: false }));
    }, 3000);
  };

  const handleLogout = async () => {
    setLoading(true);

    try {
      await authService.logout();
      showToast("Logged out successfully", "success");

      setTimeout(() => {
        navigate("/");
      }, 1000);
    } catch (error) {
      console.error("Logout error:", error);
      const errorMessage = error.response?.data?.message || "Failed to log out. Please try again.";
      showToast(errorMessage, "error");
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-10 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Profile</span>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-white">Settings</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Tune SpendWise to match your habits, accessibility needs, and account preferences.
        </p>
      </div>

      <div className="space-y-6">
        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Account</p>
          <div className="mt-4 flex items-center justify-between rounded-2xl border border-gray-100 dark:border-gray-700 p-4">
            <div className="flex items-center gap-4">
              <span className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700 text-2xl text-gray-600 dark:text-gray-300">
                <FaUser />
              </span>
              <div>
                <p className="text-base font-semibold text-gray-900 dark:text-white">Profile & Security</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">Update your details, password, and security options.</p>
              </div>
            </div>
            <Link to="/account" className="text-gray-400 dark:text-gray-500 transition-colors hover:text-gray-600 dark:hover:text-gray-300">
              <FaChevronRight size={18} />
            </Link>
          </div>
        </section>

        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Preferences</p>
          <ul className="mt-4 divide-y divide-gray-100 dark:divide-gray-700">
            {preferenceToggles.map((toggle) => (
              <li key={toggle.id} className="flex items-center justify-between gap-4 py-4">
                <div className="flex items-center gap-4">
                  <span className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700 text-lg text-gray-600 dark:text-gray-300">
                    <toggle.icon />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-gray-900 dark:text-white">{toggle.label}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{toggle.description}</p>
                  </div>
                </div>
                <input
                  type="checkbox"
                  className="toggle toggle-sm toggle-neutral"
                  checked={toggle.value}
                  onChange={toggle.onToggle}
                  aria-label={toggle.label}
                />
              </li>
            ))}
          </ul>
        </section>

        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Tools</p>
          <Link
            to="/currency-conversion"
            className="mt-4 flex items-center justify-between rounded-2xl border border-gray-100 dark:border-gray-700 p-4 transition-colors hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <div className="flex items-center gap-4">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700 text-lg text-gray-600 dark:text-gray-300">
                <FaDollarSign />
              </span>
              <div>
                <p className="text-sm font-semibold text-gray-900 dark:text-white">Currency Conversion</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">Calculate balances in the currencies you monitor.</p>
              </div>
            </div>
            <FaChevronRight className="text-gray-400 dark:text-gray-500" />
          </Link>
        </section>

        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Session</p>
          <div className="mt-4 rounded-2xl border border-gray-100 dark:border-gray-700 p-6 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Ready for a reset? You'll be signed out immediately and can log back in whenever you're ready.
            </p>
            <button
              type="button"
              className="mt-6 w-full rounded-full bg-rose-500 dark:bg-rose-600 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-rose-600 dark:hover:bg-rose-700 disabled:opacity-70"
              onClick={handleLogout}
              disabled={loading}
            >
              {loading ? (
                <span className="flex items-center justify-center">
                  <svg className="w-5 h-5 mr-2 animate-spin" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Logging Out...
                </span>
              ) : "Log Out"}
            </button>
          </div>
        </section>
      </div>

      {toast.show && (
        <div className="toast toast-top toast-end z-50">
          <div
            className={`alert shadow-lg ${
              toast.type === "success"
                ? "alert-success"
                : toast.type === "error"
                ? "alert-error"
                : toast.type === "warning"
                ? "alert-warning"
                : "alert-info"
            }`}
          >
            <div className="flex items-center gap-2 text-sm">
              <ToastIcon className="text-lg" />
              <span>{toast.message}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
