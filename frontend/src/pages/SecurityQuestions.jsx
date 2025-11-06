import React, { useState, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { securityQuestionsService, registerService } from "../services/api";

export default function SecurityQuestions() {
  const navigate = useNavigate();
  const location = useLocation();
  const userData = location.state;

  const [questions, setQuestions] = useState([]);
  const [selectedQuestionId, setSelectedQuestionId] = useState("");
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [fetchingQuestions, setFetchingQuestions] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!userData || !userData.username || !userData.email || !userData.password) {
      navigate("/signup");
      return;
    }

    const fetchQuestions = async () => {
      setError("");
      setFetchingQuestions(true);
      try {
        const response = await securityQuestionsService.getAllQuestions();
        const availableQuestions = response.data ?? [];
        if (availableQuestions.length === 0) {
          setError("No security questions available. Please contact support.");
        }
        setQuestions(availableQuestions);
      } catch (err) {
        console.error("Failed to fetch security questions:", err);
        setError("Unable to load security questions right now. Please try again later.");
      } finally {
        setFetchingQuestions(false);
      }
    };

    fetchQuestions();
  }, [userData, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const registerData = {
        username: userData.username,
        email: userData.email,
        password: userData.password,
        phoneNumber: userData.phoneNumber || null,
        questionId: parseInt(selectedQuestionId, 10),
        answer: answer.trim(),
      };

      await registerService.register(registerData);

      navigate("/login", {
        state: { message: "Registration successful! Please log in." },
      });
    } catch (err) {
      console.error("Registration failed:", err);
      const message = err.response?.data || "Registration failed. Please try again.";
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  if (fetchingQuestions) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="text-center">
          <div className="mx-auto h-12 w-12 rounded-full border-2 border-gray-300 border-t-gray-800 dark:border-gray-700 dark:border-t-white animate-spin"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-300">Loading security questions...</p>
        </div>
      </div>
    );
  }

  if (error && questions.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950 px-4">
        <div className="w-full max-w-md rounded-xl bg-white dark:bg-gray-900 p-8 shadow-md text-center">
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-gray-100">Security Questions</h1>
          <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-6 inline-flex items-center justify-center rounded-md bg-gray-800 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-200"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950 px-4">
      <div className="w-full max-w-xl rounded-xl bg-white dark:bg-gray-900 shadow-md p-8">
        <div className="text-center mb-6">
          <h1 className="text-2xl md:text-3xl font-bold text-gray-800 dark:text-gray-100">Security Question</h1>
          <p className="mt-2 text-gray-600 dark:text-gray-300">
            Choose a security question to help recover your account if needed.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {error && questions.length > 0 && (
            <div className="rounded-md border border-red-300 bg-red-100 px-3 py-2 text-sm text-red-700 dark:border-red-500 dark:bg-red-500/20 dark:text-red-200">
              {error}
            </div>
          )}

          <div className="grid gap-4">
            <div>
              <label htmlFor="securityQuestion" className="sr-only">
                Security Question
              </label>
              <select
                id="securityQuestion"
                value={selectedQuestionId}
                onChange={(e) => setSelectedQuestionId(e.target.value)}
                required
                className="w-full rounded-md border border-gray-300 bg-white px-3 py-3 text-gray-800 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
              >
                <option value="">Select a security question</option>
                {questions.map((q) => (
                  <option key={q.id} value={q.id}>
                    {q.questionText}
                  </option>
                ))}
              </select>
              <input
                type="text"
                placeholder="Your answer"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                required
                className="mt-3 w-full rounded-md border border-gray-300 px-3 py-3 text-gray-800 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-gray-800 px-4 py-3 text-white transition hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50 dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-200 dark:focus:ring-offset-gray-900"
          >
            {loading ? "Creating Account..." : "Complete Registration"}
          </button>
        </form>

        <div className="text-center mt-4">
          <p className="text-sm text-gray-600 dark:text-gray-300">
            Already have an account?{" "}
            <Link to="/login" className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300">
              Login here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
