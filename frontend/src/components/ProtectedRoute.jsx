import React, { useState, useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { userService } from '../services/api';

const ProtectedRoute = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const location = useLocation();

  useEffect(() => {
    // 验证用户是否已登录
    const checkAuthStatus = async () => {
      try {
        await userService.getCurrentUser();
        setIsAuthenticated(true);
      } catch (error) {
        console.log('User not authenticated:', error);
        setIsAuthenticated(false);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuthStatus();
  }, []);

  // 显示加载状态
  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <svg className="mx-auto h-12 w-12 animate-spin text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p className="mt-4 text-gray-600">Verifying your account...</p>
        </div>
      </div>
    );
  }

  // 如果用户未登录，重定向到登录页面
  if (isAuthenticated === false) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 如果用户已登录，渲染子路由
  return <Outlet />;
};

export default ProtectedRoute;