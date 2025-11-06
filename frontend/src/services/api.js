import axios from "axios";

// Create axios instance - no longer using complete base URL
const api = axios.create({
  // Remove baseURL because we use relative paths
  timeout: 10000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// Authentication related APIs
export const authService = {
  login: (identifier, password) => {
    return api.post("/api/auth/login", { identifier, password });
  },
  logout: () => {
    return api.post("/api/auth/logout");
  },
  getCurrentUser: () => {
    return api.get("/api/auth/me");
  },
};

// register api
export const registerService = {
  register: (registerData) => {
    // registerData should contain: username, email, password, phoneNumber (optional), securityQuestion, securityAnswer
    return api.post("/api/register", registerData);
  },
};

// user profile api
export const userService = {
  // Get current user information - using /api/auth/me path
  getCurrentUser: () => {
    return api.get("/api/auth/me");
  },

  // Update user profile
  updateProfile: (userData) => {
    return api.put("/api/users/myself", userData);
  },

  // Upload avatar - using /api/users/myself/picture path
  uploadAvatar: (formData) => {
    return api.post("/api/users/myself/picture", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },
};

// password related apis
export const passwordResetService = {
  // security questions for a given identifier (username or email)
  requestSecurityQuestion: (identifier) => {
    return api.post("/api/reset-password/request", { identifier });
  },

  // confirm password reset with security answer and new password
  confirmReset: (identifier, questionId, answer, newPassword) => {
    return api.post("/api/reset-password/confirm", {
      identifier,
      questionId,
      answer,
      newPassword,
    });
  },
  
  // change password for logged in users
  changePassword: (currentPassword, newPassword) => {
    return api.post("/api/reset-password/change", {
      currentPassword,
      newPassword
    });
  }
};

// expense record apis
export const expenseRecordService = {
  getAllRecords: async () => {
    const response = await api.get("/api/records");
    return response.data;
  },

  searchRecords: async (params) => {
    // includes filtering, pagination, and sorting
    const response = await api.get("/api/records/search", { params });
    return response.data;
  },

  createRecord: async (recordData, frequency = null) => {
    // includes one-time and recurring records
    const params = frequency ? { frequency } : {};
    const response = await api.post("/api/records", recordData, { params });
    return response.data;
  },

  updateRecord: async (id, recordData, frequency = null) => {
    const params = frequency ? { frequency } : {};
    const response = await api.put(`/api/records/${id}`, recordData, { params });
    return response.data;
  },

  deleteRecord: async (id, cancelRecurring = false) => {
    const response = await api.delete(`/api/records/${id}`, {
      params: { cancelRecurring },
    });
    return response.data;
  },

  getWeeklyReport: (year, week) => {
    return api.get("/api/records/reports/weekly", {
      params: { year, week },
    });
  },

  getMonthlyReport: (year, month) => {
    return api.get("/api/records/reports/monthly", {
      params: { year, month },
    });
  },

  getYearlyReport: (year) => {
    return api.get("/api/records/reports/yearly", {
      params: { year },
    });
  },
};

// create goals api
export const goalsService = {
  // include list, create, update, delete goals
  listActiveGoals: async () => {
    const response = await api.get("/api/goals");
    return response.data;
  },

  listProgress: async () => {
    const response = await api.get("/api/goals/progress");
    return response.data;
  },

  getGoalProgress: async (goalId) => {
    const response = await api.get(`/api/goals/${goalId}/progress`);
    return response.data;
  },

  createGoal: async (goalData) => {
    // include name, targetAmount, category, deadline
    const response = await api.post("/api/goals", goalData);
    return response.data;
  },

};

// ai suggestions api
export const aiSuggestionsService = {
  generateSuggestions: (month = null) => {
    // format of month: "YYYY-MM"
    const params = month ? { month } : {};
    return api.post("/api/suggestions/generate", null, { params });
  },
};

// security questions api
export const securityQuestionsService = {
  getAllQuestions: () => {
    return api.get("/api/security-questions");
  },
};

// achievements api
export const achievementsService = {
  getUserAchievements: () => {
    return api.get("/api/achievements");
  }
};

export default api;
