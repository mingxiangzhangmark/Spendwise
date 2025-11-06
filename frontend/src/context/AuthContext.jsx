import React, { createContext, useContext, useState, useEffect } from 'react';
import { userService, authService } from '../services/api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Check if user is already logged in
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // First check for any stored token/session indicator
        const hasSession = localStorage.getItem('hasSession') === 'true';
        
        // Only make the API call if there's a potential session
        if (hasSession) {
          const response = await userService.getCurrentUser();
          setUser(response.data);
          setIsAuthenticated(true);
        } else {
          // If no session indicator, assume not logged in without making API call
          setUser(null);
          setIsAuthenticated(false);
        }
      } catch (error) {
        // Handle error - user is not authenticated
        setUser(null);
        setIsAuthenticated(false);
        // Remove session indicator if API call fails
        localStorage.removeItem('hasSession');
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, []);

  // Login method
  const login = async (identifier, password) => {
    try {
      // 1. Send login request
      const loginResponse = await authService.login(identifier, password);
      
      // 2. After successful login, get user information
      // Some APIs return user info in the login response, some need an extra request
      let userData;
      
      if (loginResponse.data && loginResponse.data.user) {
        userData = loginResponse.data.user;
      } else {
        // If no user data in login response, fetch it separately
        const userResponse = await userService.getCurrentUser();
        userData = userResponse.data;
      }
      
      // 3. Update state
      setUser(userData);
      setIsAuthenticated(true);
      
      // Set session indicator
      localStorage.setItem('hasSession', 'true');
      
      console.log("Login successful, user data:", userData);
      
      return loginResponse;
    } catch (error) {
      console.error("Login failed in AuthContext:", error);
      throw error;
    }
  };

  // Logout method
  const logout = async () => {
    try {
      const response = await authService.logout();
      
      // Make sure state is cleared
      setUser(null);
      setIsAuthenticated(false);
      
      // Remove session indicator
      localStorage.removeItem('hasSession');
      
      console.log("Logout successful");
      
      return response;
    } catch (error) {
      console.error("Logout failed in AuthContext:", error);
      
      // Even if the API call fails, clear the local state
      setUser(null);
      setIsAuthenticated(false);
      localStorage.removeItem('hasSession');
      
      throw error;
    }
  };

  // Method to refresh user information
  const refreshUser = async () => {
    try {
      const response = await userService.getCurrentUser();
      setUser(response.data);
      setIsAuthenticated(true);
      return response.data;
    } catch (error) {
      console.error("Failed to refresh user:", error);
      throw error;
    }
  };

  const contextValue = {
    user,
    loading,
    isAuthenticated,
    login,
    logout,
    refreshUser
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}