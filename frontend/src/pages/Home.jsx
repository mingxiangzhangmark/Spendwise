import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
// eslint-disable-next-line no-unused-vars
import { motion } from "framer-motion";

// SVG图标组件
function FinanceHero({ className = "w-52 h-52" }) {
  return (
    <svg
      className={className}
      viewBox="0 0 207 207"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* SVG路径保持不变 */}
    </svg>
  );
}

// 图标组件
const ChartIcon = () => (
  <svg className="w-10 h-10 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
  </svg>
);

const AIIcon = () => (
  <svg className="w-10 h-10 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
  </svg>
);

const GoalIcon = () => (
  <svg className="w-10 h-10 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
  </svg>
);

export default function Home() {
  const [isVisible, setIsVisible] = useState(false);
  
  useEffect(() => {
    setIsVisible(true);
  }, []);
  
  // 动画变体
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: { 
        duration: 0.8,
        when: "beforeChildren",
        staggerChildren: 0.2
      }
    }
  };
  
  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { 
      y: 0, 
      opacity: 1,
      transition: { duration: 0.5 }
    }
  };
  
  return (
    <div className="bg-gradient-to-b from-white to-blue-50 dark:from-gray-900 dark:to-gray-800 min-h-screen transition-colors duration-200">

      {/* <nav className="bg-white py-4 px-6 shadow-sm fixed w-full top-0 z-50">
        <div className="max-w-7xl mx-auto flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 flex items-center justify-center">
              <span className="text-white font-bold">SW</span>
            </div>
            <span className="text-xl font-bold text-gray-800">SpendWise</span>
          </div>
          <div className="space-x-4">
            <Link to="/login" className="px-4 py-2 text-indigo-600 font-medium hover:text-indigo-800 transition">
              Log In
            </Link>
            <Link to="/signup" className="px-4 py-2 bg-indigo-600 text-white rounded-md font-medium hover:bg-indigo-700 transition">
              Sign Up
            </Link>
          </div>
        </div>
      </nav> */}

      {/* 英雄区块 */}
      <motion.div 
        className="pt-24 pb-16 px-4 sm:px-6 lg:pt-32 lg:pb-24"
        initial="hidden"
        animate={isVisible ? "visible" : "hidden"}
        variants={containerVariants}
      >
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <motion.div variants={itemVariants} className="text-center lg:text-left">
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-gray-900 dark:text-white tracking-tight transition-colors duration-200">
                <span className="block">Simplify Your</span>
                <span className="block text-indigo-600 dark:text-indigo-400">Financial Journey</span>
              </h1>
              <p className="mt-6 text-lg sm:text-xl text-gray-600 dark:text-gray-300 max-w-xl mx-auto lg:mx-0 transition-colors duration-200">
                Track, visualize, and optimize your finances in one place. Get AI-powered insights and reach your financial goals faster.
              </p>
              <div className="mt-8 flex flex-col sm:flex-row justify-center lg:justify-start gap-4">
                <Link
                  to="/signup"
                  className="px-8 py-3 bg-indigo-600 text-white rounded-lg font-medium shadow-md hover:bg-indigo-700 transition transform hover:scale-105 focus:outline-none"
                >
                  Get Started
                </Link>
                <Link
                  to="/login"
                  className="px-8 py-3 bg-white dark:bg-gray-700 text-indigo-600 dark:text-indigo-400 border border-indigo-600 dark:border-indigo-400 rounded-lg font-medium hover:bg-indigo-50 dark:hover:bg-gray-600 transition transform hover:scale-105 focus:outline-none"
                >
                  Log In
                </Link>
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="flex justify-center">
              <div className="relative">
                <div className="absolute inset-0 bg-gradient-to-r from-indigo-300 to-purple-300 rounded-full filter blur-3xl opacity-30 animate-pulse"></div>
                <FinanceHero className="w-72 h-72 md:w-96 md:h-96 relative z-10" />
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>

      {/* 功能展示 */}
      <motion.div 
        className="py-16 bg-white dark:bg-gray-800 transition-colors duration-200"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5, duration: 0.8 }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="text-center">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white sm:text-4xl transition-colors duration-200">
              All Your Finances in One Place
            </h2>
            <p className="mt-4 text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto transition-colors duration-200">
              SpendWise brings together all your financial accounts, providing real-time insights and personalized recommendations.
            </p>
          </div>

          <div className="mt-16 grid gap-8 md:grid-cols-3">
            {/* 功能卡片 1 */}
            <motion.div 
              className="bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl p-8 shadow-sm hover:shadow-md transition"
              whileHover={{ y: -5 }}
            >
              <div className="bg-white dark:bg-gray-700 p-3 rounded-lg inline-block shadow-sm mb-4 transition-colors duration-200">
                <ChartIcon />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mt-2 transition-colors duration-200">Monitor</h3>
              <p className="mt-2 text-gray-600 dark:text-gray-300 transition-colors duration-200">
                Track all your accounts in one place. Get a unified view of your spending habits across multiple platforms and monitor your budget in real time.
              </p>
            </motion.div>

            {/* 功能卡片 2 */}
            <motion.div 
              className="bg-gradient-to-br from-purple-50 to-pink-50 dark:from-purple-900/20 dark:to-pink-900/20 rounded-xl p-8 shadow-sm hover:shadow-md transition"
              whileHover={{ y: -5 }}
            >
              <div className="bg-white dark:bg-gray-700 p-3 rounded-lg inline-block shadow-sm mb-4 transition-colors duration-200">
                <GoalIcon />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mt-2 transition-colors duration-200">Visualize</h3>
              <p className="mt-2 text-gray-600 dark:text-gray-300 transition-colors duration-200">
                Gain insights through intuitive charts and analytics. Set financial goals, track your progress, and make informed decisions based on your spending patterns.
              </p>
            </motion.div>

            {/* 功能卡片 3 */}
            <motion.div 
              className="bg-gradient-to-br from-indigo-50 to-blue-50 dark:from-indigo-900/20 dark:to-blue-900/20 rounded-xl p-8 shadow-sm hover:shadow-md transition"
              whileHover={{ y: -5 }}
            >
              <div className="bg-white dark:bg-gray-700 p-3 rounded-lg inline-block shadow-sm mb-4 transition-colors duration-200">
                <AIIcon />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mt-2 transition-colors duration-200">Optimize</h3>
              <div className="mt-2 text-gray-600 dark:text-gray-300 transition-colors duration-200">
                <p className="text-base leading-relaxed text-gray-600 dark:text-gray-400">
                Receive AI-powered recommendations tailored to your financial habits. Get personalized advice to improve your financial health and achieve your long-term spending goals.
              </p>
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>

      {/* 应用亮点 */}
      <div className="py-16 bg-gradient-to-b from-indigo-900 to-purple-900 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="text-center">
            <h2 className="text-3xl font-bold sm:text-4xl">
              Why Choose SpendWise?
            </h2>
            <p className="mt-4 text-lg text-indigo-200 max-w-2xl mx-auto">
              Our platform is designed to make financial management accessible, intuitive, and proactive.
            </p>
          </div>

          <div className="mt-12 grid gap-6 md:grid-cols-2 lg:grid-cols-4">
            <div className="p-4">
              <div className="font-bold text-3xl text-indigo-300 mb-2">01</div>
              <h3 className="font-semibold text-lg">Unified View</h3>
              <p className="mt-2 text-indigo-200">
                Connect all your accounts for a complete financial overview.
              </p>
            </div>
            
            <div className="p-4">
              <div className="font-bold text-3xl text-indigo-300 mb-2">02</div>
              <h3 className="font-semibold text-lg">Intelligent Insights</h3>
              <p className="mt-2 text-indigo-200">
                AI-powered analysis helps you understand your spending patterns.
              </p>
            </div>
            
            <div className="p-4">
              <div className="font-bold text-3xl text-indigo-300 mb-2">03</div>
              <h3 className="font-semibold text-lg">Goal Setting</h3>
              <p className="mt-2 text-indigo-200">
                Create customized financial goals with progress tracking.
              </p>
            </div>
            
            <div className="p-4">
              <div className="font-bold text-3xl text-indigo-300 mb-2">04</div>
              <h3 className="font-semibold text-lg">Personalized Advice</h3>
              <p className="mt-2 text-indigo-200">
                Receive tailored recommendations to improve your financial health.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 模拟界面展示 */}
      <div className="py-16 bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white sm:text-4xl transition-colors duration-200">
              Powerful Dashboard at Your Fingertips
            </h2>
            <p className="mt-4 text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto transition-colors duration-200">
              Track your financial journey with our intuitive and comprehensive dashboard.
            </p>
          </div>

          <div className="relative">
            <div className="rounded-2xl bg-white dark:bg-gray-800 shadow-xl overflow-hidden border border-gray-200 dark:border-gray-700 transition-colors duration-200">
              {/* 模拟的应用界面 */}
              <div className="bg-indigo-600 dark:bg-indigo-700 h-16 flex items-center px-6 transition-colors duration-200">
                <div className="w-3 h-3 rounded-full bg-red-500 mr-2"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500 mr-2"></div>
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <div className="p-6">
                <div className="grid grid-cols-3 gap-4">
                  <div className="col-span-3 sm:col-span-2">
                    <div className="bg-gray-100 dark:bg-gray-700 rounded-lg p-4 h-40 mb-4 transition-colors duration-200">
                      <div className="h-3 w-1/3 bg-indigo-200 dark:bg-indigo-400 rounded mb-2 transition-colors duration-200"></div>
                      <div className="h-2 w-full bg-indigo-100 dark:bg-indigo-500 rounded mb-2 transition-colors duration-200"></div>
                      <div className="h-2 w-full bg-indigo-100 dark:bg-indigo-500 rounded mb-2 transition-colors duration-200"></div>
                      <div className="h-20 w-full bg-indigo-50 dark:bg-indigo-600 rounded mt-4 transition-colors duration-200"></div>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-gray-100 dark:bg-gray-700 rounded-lg p-4 h-32 transition-colors duration-200">
                        <div className="h-3 w-1/2 bg-green-200 dark:bg-green-400 rounded mb-2 transition-colors duration-200"></div>
                        <div className="h-16 w-full bg-green-50 dark:bg-green-600 rounded mt-4 transition-colors duration-200"></div>
                      </div>
                      <div className="bg-gray-100 dark:bg-gray-700 rounded-lg p-4 h-32 transition-colors duration-200">
                        <div className="h-3 w-1/2 bg-red-200 dark:bg-red-400 rounded mb-2 transition-colors duration-200"></div>
                        <div className="h-16 w-full bg-red-50 dark:bg-red-600 rounded mt-4 transition-colors duration-200"></div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="col-span-3 sm:col-span-1">
                    <div className="bg-gray-100 dark:bg-gray-700 rounded-lg p-4 h-full transition-colors duration-200">
                      <div className="h-3 w-1/2 bg-purple-200 dark:bg-purple-400 rounded mb-2 transition-colors duration-200"></div>
                      <div className="h-2 w-3/4 bg-purple-100 dark:bg-purple-500 rounded mb-4 transition-colors duration-200"></div>
                      
                      <div className="h-12 w-full bg-white dark:bg-gray-600 rounded-lg shadow-sm mb-3 p-2 flex items-center transition-colors duration-200">
                        <div className="w-2 h-8 bg-green-400 dark:bg-green-500 rounded-sm mr-2 transition-colors duration-200"></div>
                        <div>
                          <div className="h-2 w-20 bg-gray-300 dark:bg-gray-500 rounded transition-colors duration-200"></div>
                          <div className="h-2 w-12 bg-gray-200 dark:bg-gray-600 rounded mt-1 transition-colors duration-200"></div>
                        </div>
                      </div>
                      
                      <div className="h-12 w-full bg-white dark:bg-gray-600 rounded-lg shadow-sm mb-3 p-2 flex items-center transition-colors duration-200">
                        <div className="w-2 h-8 bg-red-400 dark:bg-red-500 rounded-sm mr-2 transition-colors duration-200"></div>
                        <div>
                          <div className="h-2 w-24 bg-gray-300 dark:bg-gray-500 rounded transition-colors duration-200"></div>
                          <div className="h-2 w-16 bg-gray-200 dark:bg-gray-600 rounded mt-1 transition-colors duration-200"></div>
                        </div>
                      </div>
                      
                      <div className="h-12 w-full bg-white dark:bg-gray-600 rounded-lg shadow-sm p-2 flex items-center transition-colors duration-200">
                        <div className="w-2 h-8 bg-blue-400 dark:bg-blue-500 rounded-sm mr-2 transition-colors duration-200"></div>
                        <div>
                          <div className="h-2 w-16 bg-gray-300 dark:bg-gray-500 rounded transition-colors duration-200"></div>
                          <div className="h-2 w-10 bg-gray-200 dark:bg-gray-600 rounded mt-1 transition-colors duration-200"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="absolute -top-6 -left-6 w-32 h-32 bg-indigo-500 rounded-full opacity-10"></div>
            <div className="absolute -bottom-10 -right-10 w-48 h-48 bg-purple-500 rounded-full opacity-10"></div>
          </div>
        </div>
      </div>

      {/* 行动召唤 */}
      <div className="py-16 bg-white dark:bg-gray-800 transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-700 dark:to-purple-700 rounded-2xl shadow-xl p-8 md:p-12 text-white text-center transition-colors duration-200">
            <h2 className="text-3xl md:text-4xl font-bold">
              Ready to Take Control of Your Finances?
            </h2>
            <p className="mt-4 text-lg md:text-xl text-indigo-100 dark:text-indigo-200 max-w-2xl mx-auto transition-colors duration-200">
              Join thousands of users who are already simplifying their financial management and reaching their goals.
            </p>
            <div className="mt-8">
              <Link
                to="/signup"
                className="px-8 py-4 bg-white dark:bg-gray-100 text-indigo-600 dark:text-indigo-700 rounded-lg font-medium shadow-md hover:bg-indigo-50 dark:hover:bg-gray-200 transition transform hover:scale-105 focus:outline-none inline-block"
              >
                Get Started for Free
              </Link>
              <p className="mt-4 text-sm text-indigo-200 dark:text-indigo-300 transition-colors duration-200">
                No credit card required. Start tracking your finances in minutes.
              </p>
            </div>
          </div>
        </div>
      </div>


    </div>
  );
}
