import React, { useState } from "react";
import { Link } from "react-router-dom";
import { passwordResetService } from "../services/api";

export default function ForgotPassword() {
  const [identifier, setIdentifier] = useState("");
  const [question, setQuestion] = useState(null);
  const [answer, setAnswer] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleRequestQuestion = async (e) => {
    e.preventDefault();
    setError("");

    if (!identifier.trim()) {
      setError("Please enter your email or username.");
      return;
    }

    setLoading(true);
    try {
      const response = await passwordResetService.requestSecurityQuestion(identifier.trim());
      setQuestion(response.data);
      setStep(2);
    } catch (err) {
      console.error("Failed to load security question:", err);
      if (err.response?.status === 404) {
        setError("We couldn't find an account with those details. Please double-check and try again.");
      } else {
        setError("Unable to load your security question right now. Please try again later.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError("");

    if (newPassword !== confirmPassword) {
      setError("New password and confirmation do not match.");
      return;
    }

    if (!question?.id) {
      setError("Security question details are missing. Please restart the recovery process.");
      return;
    }

    setLoading(true);
    try {
      await passwordResetService.confirmReset(
        identifier.trim(),
        question.id,
        answer.trim(),
        newPassword
      );
      setSuccessMessage("Password reset successful. You can now log in with your new password.");
      setStep(3);
    } catch (err) {
      console.error("Password reset failed:", err);
      const message = err.response?.data || "Security answer incorrect. Please try again.";
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const resetFlow = () => {
    setQuestion(null);
    setAnswer("");
    setNewPassword("");
    setConfirmPassword("");
    setSuccessMessage("");
    setIdentifier("");
    setError("");
    setStep(1);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-8 dark:bg-gray-950">
      <div className="w-full max-w-lg rounded-xl bg-white p-8 shadow-md transition dark:bg-gray-900">
        <div className="text-center">
          <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-100">Account Recovery</h1>
          <p className="mt-2 text-gray-600 dark:text-gray-300">
            {step === 1
              ? "Enter your email or username to retrieve your security question."
              : step === 2
              ? "Answer your security question and choose a new password."
              : "All set! Your password has been updated."}
          </p>
        </div>

        {error && (
          <div className="mt-6 rounded-md border border-red-300 bg-red-100 px-4 py-3 text-sm text-red-700 dark:border-red-500 dark:bg-red-500/20 dark:text-red-200">
            {error}
          </div>
        )}

        <div className="mt-6">
          {step === 1 && (
            <form onSubmit={handleRequestQuestion} className="space-y-5">
              <div>
                <label htmlFor="identifier" className="sr-only">
                  Email or Username
                </label>
                <input
                  id="identifier"
                  name="identifier"
                  type="text"
                  required
                  autoComplete="username"
                  placeholder="Email or username"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  className="w-full rounded-md border border-gray-300 bg-white px-3 py-3 text-gray-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-md bg-gray-800 px-4 py-3 text-white transition hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50 dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-200 dark:focus:ring-offset-gray-900"
              >
                {loading ? "Loading..." : "Continue"}
              </button>
            </form>
          )}

          {step === 2 && question && (
            <form onSubmit={handleResetPassword} className="space-y-5">
              <div className="rounded-md border border-gray-200 bg-gray-50 px-4 py-3 text-gray-700 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200">
                <p className="text-sm uppercase tracking-wider text-gray-500 dark:text-gray-400">Security Question</p>
                <p className="mt-1 text-base font-medium">{question.questionText}</p>
              </div>

              <div>
                <label htmlFor="answer" className="sr-only">
                  Security answer
                </label>
                <input
                  id="answer"
                  name="answer"
                  type="text"
                  required
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  placeholder="Enter your answer"
                  className="w-full rounded-md border border-gray-300 bg-white px-3 py-3 text-gray-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="newPassword" className="sr-only">
                    New password
                  </label>
                  <input
                    id="newPassword"
                    name="newPassword"
                    type="password"
                    required
                    minLength={8}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="New password"
                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-3 text-gray-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
                  />
                </div>
                <div>
                  <label htmlFor="confirmPassword" className="sr-only">
                    Confirm new password
                  </label>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    required
                    minLength={8}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm password"
                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-3 text-gray-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
                  />
                </div>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <button
                  type="button"
                  onClick={resetFlow}
                  className="inline-flex items-center justify-center rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-400 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                >
                  Start over
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="inline-flex items-center justify-center rounded-md bg-gray-800 px-6 py-3 text-white transition hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50 dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-200 dark:focus:ring-offset-gray-900"
                >
                  {loading ? "Resetting..." : "Reset Password"}
                </button>
              </div>
            </form>
          )}

          {step === 3 && (
            <div className="text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-500/20 dark:text-green-300">
                <svg
                  className="h-10 w-10"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>
              <h2 className="mt-4 text-2xl font-semibold text-gray-900 dark:text-gray-100">Password Reset!</h2>
              <p className="mt-2 text-gray-600 dark:text-gray-300">{successMessage}</p>
              <div className="mt-6 space-y-3">
                <Link
                  to="/login"
                  className="block w-full rounded-md bg-gray-800 px-4 py-3 text-center text-white transition hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-200 dark:focus:ring-offset-gray-900"
                >
                  Back to Login
                </Link>
                <button
                  onClick={resetFlow}
                  className="w-full rounded-md border border-gray-300 px-4 py-3 text-sm text-gray-700 transition hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-400 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                >
                  Reset another account
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="mt-8 text-center">
          <p className="text-sm text-gray-600 dark:text-gray-300">
            Remember your password?{" "}
            <Link to="/login" className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300">
              Back to login
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
