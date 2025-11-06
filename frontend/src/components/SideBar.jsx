import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  FaHome,
  FaChartLine,
  FaBullseye,
  FaChartBar,
  FaLightbulb,
  FaTrophy,
  FaCog,
  FaChevronLeft,
  FaChevronRight,
} from "react-icons/fa";

export default function SideBar() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation(); // This hook gives you the current location object

  const menuItems = [
    { name: "Dashboard", icon: <FaHome />, path: "/dashboard" },
    { name: "Expense", icon: <FaChartLine />, path: "/expense" },
    { name: "Spending Goals", icon: <FaBullseye />, path: "/goals" },
    { name: "Reports", icon: <FaChartBar />, path: "/reports" },
    { name: "Suggestions", icon: <FaLightbulb />, path: "/suggestions" },
    { name: "Achievements", icon: <FaTrophy />, path: "/achievements" },
    { name: "Setting", icon: <FaCog />, path: "/settings" },
  ];

  const toggleSidebar = () => {
    setCollapsed(!collapsed);
  };

  return (
    <div
      className={`h-screen ${collapsed ? "w-16" : "w-64"} bg-gray-100 dark:bg-gray-800 transition-all duration-300 flex flex-col`}
    >
      <div className="p-4 flex justify-between items-center border-b border-gray-200 dark:border-gray-700">
        <button
          onClick={toggleSidebar}
          className="text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white focus:outline-none"
        >
          {collapsed ? (
            <FaChevronRight size={20} />
          ) : (
            <FaChevronLeft size={20} />
          )}
        </button>
        {!collapsed && <span className="font-medium text-gray-800 dark:text-gray-200">Menu</span>}
      </div>

      <nav className="flex-1">
        <ul className="py-2">
          {menuItems.map((item, index) => (
            <li key={index}>
              <Link
                to={item.path}
                className={`flex items-center px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-700 dark:hover:text-blue-400 transition-colors ${
                  location.pathname === item.path
                    ? "bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 border-l-4 border-blue-500 dark:border-blue-400"
                    : ""
                }`}
              >
                <div className={`text-xl ${collapsed ? "mx-auto" : "mr-4"}`}>
                  {item.icon}
                </div>
                {!collapsed && <span>{item.name}</span>}
              </Link>
            </li>
          ))}
        </ul>
      </nav>

      {/* <div className="p-4 border-t border-gray-200">
        <div
          className={`flex ${collapsed ? "justify-center" : "items-center"}`}
        >
          {collapsed ? (
            <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white">
              U
            </div>
          ) : (
            <>
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white mr-3">
                U
              </div>
              <div>
                <p className="text-sm font-medium text-gray-700">User Name</p>
                <p className="text-xs text-gray-500">user@example.com</p>
              </div>
            </>
          )}
        </div>
      </div> */}
    </div>
  );
}
