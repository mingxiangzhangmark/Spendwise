import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { FaEnvelope, FaLock, FaEye, FaEyeSlash, FaCheckCircle, FaExclamationCircle, FaInfoCircle, FaExclamationTriangle } from "react-icons/fa";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [toast, setToast] = useState({ show: false, message: "", type: "info" });
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const TOAST_ICON = {
    success: FaCheckCircle,
    error: FaExclamationCircle,
    warning: FaExclamationTriangle,
    info: FaInfoCircle,
  };

  const ToastIcon = toast.type ? TOAST_ICON[toast.type] : TOAST_ICON.info;

  const from = location.state?.from?.pathname || "/dashboard";

  const showToast = useCallback((message, type = "info") => {
    setToast({ show: true, message, type });
    setTimeout(() => {
      setToast((prev) => ({ ...prev, show: false }));
      
      if (type === "success") {
        navigate(from, { replace: true });
      }
    }, 3000);
  }, [navigate, from]);

  // Show registration success message if coming from registration
  useEffect(() => {
    if (location.state?.message) {
      showToast(location.state.message, "success");
      // Clear the message from location state
      window.history.replaceState({}, document.title);
    }
  }, [location.state, showToast]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    
    try {
      await login(identifier, password);
      
      showToast("Login successful!", "success");
      
      setTimeout(() => {
        navigate(from, { replace: true });
      }, 1500);
    } catch (error) {
      console.error("Login error:", error);
      const errorMessage = error.response?.data?.message || "Login failed. Please check your credentials.";
      showToast(errorMessage, "error");
      setLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      <div className="w-full max-w-md p-8 space-y-8 bg-white dark:bg-gray-800 rounded-lg shadow-md animate-fade-in transition-colors duration-200">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-800 dark:text-white transition-colors duration-200">Login Here</h1>
          <p className="mt-2 text-gray-600 dark:text-gray-300 transition-colors duration-200">
            Welcome back, you've been missed!
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label htmlFor="identifier" className="sr-only">
                Email or Username
              </label>
              <input
                id="identifier"
                name="identifier"
                type="text"
                required
                className="w-full px-3 py-3 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 transition-colors duration-200"
                placeholder="Email or Username"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
              />
            </div>

            <div className="relative">
              <label htmlFor="password" className="sr-only">
                Password
              </label>
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                required
                className="w-full px-3 py-3 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 transition-colors duration-200"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 focus:outline-none transition-colors duration-200"
                onClick={togglePasswordVisibility}
              >
                {showPassword ? (
                  <FaEye className="h-5 w-5" />
                ) : (
                  <FaEyeSlash className="h-5 w-5" />
                )}
              </button>
            </div>
          </div>

          <div className="flex justify-end">
            <Link
              to="/forgot-password"
              className="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 transition-colors duration-200"
            >
              Forgot your password?
            </Link>
          </div>

          <button
            type="submit"
            className="w-full px-4 py-3 text-white bg-gray-800 dark:bg-gray-700 rounded-md hover:bg-gray-700 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-gray-500 dark:focus:ring-gray-400 focus:ring-offset-2 transition-colors cursor-pointer disabled:opacity-50"
            disabled={loading}
          >
            {loading ? (
              <span className="flex items-center justify-center">
                <svg
                  className="w-5 h-5 mr-2 animate-spin"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Signing in...
              </span>
            ) : (
              "Sign in"
            )}
          </button>
        </form>

        <div className="text-center mt-4">
          <p className="text-sm text-gray-600 dark:text-gray-300 transition-colors duration-200">
            Don't have an account?{" "}
            <Link to="/signup" className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 transition-colors duration-200">
              Signup here
            </Link>
          </p>
        </div>
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
