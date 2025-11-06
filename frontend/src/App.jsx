import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Login from "./pages/Login";
import SecurityQuestions from "./pages/SecurityQuestions";
import Dashboard from "./pages/Dashboard";
import ForgotPassword from "./pages/ForgotPassword";
import Signup from "./pages/Signup";
import Expense from "./pages/Expense";
import Goals from "./pages/Goals";
import Reports from "./pages/Reports";
import Achievements from "./pages/Achievements";
import Settings from "./pages/Settings";
import DashboardLayout from "./components/DashboardLayout";
import Suggestions from "./pages/Suggestions";
import Header from "./components/Header";
import Footer from "./components/Footer";
import Account from "./pages/Account";
import CurrencyConversion from "./pages/CurrencyConversion";
import { FontProvider } from "./context/FontContext";
import { AuthProvider } from "./context/AuthContext";
import { DarkModeProvider } from "./context/DarkModeContext";
import ProtectedRoute from "./components/ProtectedRoute"; 

function App() {
  return (
    <>
      <BrowserRouter>
        <DarkModeProvider>
          <FontProvider>
            <AuthProvider>
              <Header />
              <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/security-questions" element={<SecurityQuestions />} />
              
              {/* 使用ProtectedRoute保护需要登录的路由 */}
              <Route element={<ProtectedRoute />}>
                {/* Dashboard相关页面包装在DashboardLayout中 */}
                <Route element={<DashboardLayout />}>
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/expense" element={<Expense />} />
                  <Route path="/goals" element={<Goals />} />
                  <Route path="/reports" element={<Reports />} />
                  <Route path="/suggestions" element={<Suggestions />} />
                  <Route path="/achievements" element={<Achievements />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="/account" element={<Account />} />
                  <Route path="/currency-conversion" element={<CurrencyConversion />} />
                </Route>
              </Route>

              <Route path="*" element={<div>404 Not Found</div>} />
            </Routes>
            <Footer />
          </AuthProvider>
        </FontProvider>
      </DarkModeProvider>
    </BrowserRouter>
    </>
  );
}

export default App;
