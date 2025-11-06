import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaUser, FaTachometerAlt, FaSignOutAlt } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext'; // 导入useAuth

export default function Header() {
  const { user, loading, logout } = useAuth(); // 使用AuthContext提供的状态和方法
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const navigate = useNavigate();

  // 获取Header高度并设置页面内容的padding
  useEffect(() => {
    const header = document.querySelector('nav');
    if (header) {
      const headerHeight = header.offsetHeight;
      document.body.style.paddingTop = `${headerHeight}px`;
    }
    
    return () => {
      // 清理函数
      document.body.style.paddingTop = '0';
    };
  }, []);

  // 切换下拉菜单状态
  const toggleDropdown = () => {
    setDropdownOpen(!dropdownOpen);
  };

  // 点击外部关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownOpen && !event.target.closest('.dropdown')) {
        setDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [dropdownOpen]);

  // 处理登出
  const handleLogout = async () => {
    try {
      await logout(); // 使用AuthContext提供的logout方法
      setDropdownOpen(false);
      navigate('/');
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  // 获取用户头像或初始字母
  const getUserAvatar = () => {
    if (user?.profilePictureUrl) {
      return (
        <img 
          src={user.profilePictureUrl} 
          alt={user.username} 
          className="w-8 h-8 rounded-full object-cover"
        />
      );
    } else {
      const initial = user?.username ? user.username.charAt(0).toUpperCase() : 'U';
      return (
        <div className="w-8 h-8 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 flex items-center justify-center text-white font-medium">
          {initial}
        </div>
      );
    }
  };

  return (
    <nav className="bg-white dark:bg-gray-900 py-4 px-6 shadow-sm dark:shadow-gray-800 fixed w-full top-0 z-50 transition-colors duration-200">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
     
        <Link to="/" className="flex items-center space-x-2">
          <div className="w-10 h-10 rounded-full bg-indigo-600 dark:bg-indigo-500 flex items-center justify-center">
            <span className="text-white font-bold">SW</span>
          </div>
          <span className="text-xl font-bold text-gray-800 dark:text-white">SpendWise</span>
        </Link>

   
        <div className="flex items-center space-x-4">
          {loading ? (
         
            <div className="w-24 h-10 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
          ) : user ? (
        
            <div className="relative dropdown">
              <button 
                onClick={toggleDropdown}
                className="flex items-center space-x-3 focus:outline-none"
                aria-label="User menu"
              >
             
                <span className="hidden md:block text-gray-700 dark:text-gray-300 font-medium">
                  {user.username}
                </span>
                {getUserAvatar()}
              </button>
              
              {dropdownOpen && (
                <div className="absolute right-0 mt-2 w-52 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50">
           
                  <div className="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 border-b border-gray-100 dark:border-gray-700 md:hidden">
                    <span>Signed in as</span>
                    <p className="font-semibold text-gray-800 dark:text-gray-200">{user.username}</p>
                  </div>
                  
                  <Link 
                    to="/dashboard" 
                    className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                    onClick={() => setDropdownOpen(false)}
                  >
                    <FaTachometerAlt className="text-gray-500 dark:text-gray-400" />
                    Dashboard
                  </Link>
                  
                  <Link 
                    to="/settings" 
                    className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                    onClick={() => setDropdownOpen(false)}
                  >
                    <FaUser className="text-gray-500 dark:text-gray-400" />
                    Account Settings
                  </Link>
                  
                  <hr className="my-1 border-gray-200 dark:border-gray-700" />
                  
                  <button 
                    onClick={handleLogout}
                    className="flex items-center gap-3 px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700 w-full text-left"
                  >
                    <FaSignOutAlt className="text-red-500 dark:text-red-400" />
                    Sign Out
                  </button>
                </div>
              )}
            </div>
          ) : (
        
            <>
              <Link to="/login" className="text-indigo-600 dark:text-indigo-400 font-medium hover:text-indigo-800 dark:hover:text-indigo-300 transition">
                Log In
              </Link>
              <Link to="/signup" className="px-6 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-lg font-medium hover:bg-indigo-700 dark:hover:bg-indigo-600 transition">
                Sign Up
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
