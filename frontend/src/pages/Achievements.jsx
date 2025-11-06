import React, { useState, useEffect } from 'react';
import { 
  FaTrophy, 
  FaMedal, 
  FaStar, 
  FaUserPlus, 
  FaMoneyBillWave,
  FaChartLine,
  FaListAlt,
  FaCalendarCheck,
  FaLock
} from 'react-icons/fa';
import { achievementsService } from '../services/api';

// Map achievement codes to appropriate icons
const achievementIcons = {
  'ACCOUNT_CREATED': <FaUserPlus className="text-blue-500" />,
  'FIRST_EXPENSE': <FaMoneyBillWave className="text-green-500" />,
  'TEN_RECORDS': <FaListAlt className="text-purple-500" />,
  'BUDGET_MASTER': <FaChartLine className="text-red-500" />,
  'GOAL_COMPLETE': <FaCalendarCheck className="text-orange-500" />,
  // Add more mappings as needed
  // Default icon for any unmapped achievement codes
  'DEFAULT': <FaTrophy className="text-yellow-500" />
};

// Format date to a more readable format
const formatDate = (dateString) => {
  if (!dateString) return 'Not yet earned';
  
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

export default function Achievements() {
  const [achievements, setAchievements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAchievements = async () => {
      try {
        setLoading(true);
        const response = await achievementsService.getUserAchievements();
        setAchievements(response.data);
        setError(null);
      } catch (err) {
        console.error("Failed to fetch achievements:", err);
        setError("Failed to load achievements. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchAchievements();
  }, []);

  // Get icon for an achievement code
  const getAchievementIcon = (code) => {
    return achievementIcons[code] || achievementIcons['DEFAULT'];
  };

  // Sort achievements by earned status (earned first) and then by earnedAt date (newest first)
  const sortedAchievements = [...achievements].sort((a, b) => {
    if (a.earned && !b.earned) return -1;
    if (!a.earned && b.earned) return 1;
    if (a.earned && b.earned) {
      return new Date(b.earnedAt) - new Date(a.earnedAt); // newest first
    }
    return 0;
  });

  // Progress based on a fixed total of 4 goals
  const TOTAL_GOALS = 4;
  const earnedCount = new Set(
    achievements.filter(a => a.earned).map(a => a.achievement?.code)
  ).size;
  const progressPct = Math.min(100, Math.max(0, (earnedCount / TOTAL_GOALS) * 100));

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg bg-red-50 p-4 text-red-600 max-w-4xl mx-auto my-8">
        <p>{error}</p>
      </div>
    );
  }

  return (
  <div className="max-w-6xl mx-auto p-4 text-gray-900 dark:text-gray-100">
      <div className="mb-8 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Progress</span>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-100">Your Achievements</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Track your progress and celebrate your financial milestones.
        </p>
      </div>

      {/* Progress summary */}
      <div className="mb-8 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-800 p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Progress Summary</h2>
          <div className="rounded-full bg-indigo-50 dark:bg-indigo-900/40 px-3 py-1 text-sm font-medium text-indigo-700 dark:text-indigo-300">
            {earnedCount} / {TOTAL_GOALS} Achieved
          </div>
        </div>
        
        {/* Progress bar */}
        <div className="h-3 w-full rounded-full bg-gray-100 dark:bg-gray-700">
          <div 
            className="h-3 rounded-full bg-indigo-600 dark:bg-indigo-500 transition-all duration-500"
            style={{ width: `${progressPct}%` }}
          ></div>
        </div>
      </div>

      {/* Achievements grid */}
      <div className="grid gap-4 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
        {sortedAchievements.length > 0 ? (
          sortedAchievements.map((achievement) => (
            <div 
              key={achievement.id} 
              className={`flex flex-col rounded-2xl border p-5 ${
                achievement.earned 
                  ? "border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-800" 
                  : "border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/50"
              }`}
            >
              <div className="mb-4 flex items-center gap-4">
                <div className={`flex h-12 w-12 items-center justify-center rounded-full ${
                  achievement.earned 
                    ? "bg-indigo-100 dark:bg-indigo-900/40" 
                    : "bg-gray-100 dark:bg-gray-700"
                }`}>
                  {achievement.earned ? (
                    getAchievementIcon(achievement.achievement.code)
                  ) : (
                    <FaLock className="text-gray-400" />
                  )}
                </div>
                <div>
                  <h3 className={`font-semibold ${
                    achievement.earned ? "text-gray-900 dark:text-gray-100" : "text-gray-500 dark:text-gray-400"
                  }`}>
                    {achievement.achievement.title}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {achievement.earned ? (
                      formatDate(achievement.earnedAt)
                    ) : (
                      'Locked'
                    )}
                  </p>
                </div>
              </div>
              
              <p className={`text-sm ${
                achievement.earned ? "text-gray-600 dark:text-gray-300" : "text-gray-400 dark:text-gray-500"
              }`}>
                {achievement.achievement.description}
              </p>
              
              {achievement.earned && (
                <div className="mt-4 flex items-center text-xs text-indigo-600 dark:text-indigo-400">
                  <FaStar className="mr-1" />
                  <span>Achievement unlocked!</span>
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="col-span-3 py-8 text-center text-gray-500 dark:text-gray-400">
            <p>No achievements available yet. Keep using the app to earn achievements!</p>
          </div>
        )}
      </div>
    </div>
  );
}
