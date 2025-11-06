import React from "react";
import { Outlet } from "react-router-dom";
import SideBar from "./SideBar";

export default function DashboardLayout() {
  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      <SideBar />
      <div className="flex-1 p-8 overflow-auto">
        <Outlet />
      </div>
    </div>
  );
}
