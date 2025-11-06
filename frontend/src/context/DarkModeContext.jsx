import React, { createContext, useContext, useState, useEffect } from 'react';

const DarkModeContext = createContext();

export function DarkModeProvider({ children }) {
  // Check localStorage for saved dark mode preference
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem('darkMode');
    const initialValue = saved ? JSON.parse(saved) : false;
    console.log('🌙 DarkMode initial value:', initialValue);
    return initialValue;
  });

  // Apply dark mode class to document element when state changes
  useEffect(() => {
    console.log('🌙 DarkMode changed to:', darkMode);
    localStorage.setItem('darkMode', JSON.stringify(darkMode));
    
    if (darkMode) {
      document.documentElement.classList.add('dark');
      console.log('✅ Added dark class to html element');
      console.log('HTML classes:', document.documentElement.className);
    } else {
      document.documentElement.classList.remove('dark');
      console.log('❌ Removed dark class from html element');
      console.log('HTML classes:', document.documentElement.className);
    }
  }, [darkMode]);

  const toggleDarkMode = () => {
    console.log('🔄 Toggling dark mode from:', darkMode);
    setDarkMode(prev => !prev);
  };

  return (
    <DarkModeContext.Provider value={{ darkMode, setDarkMode, toggleDarkMode }}>
      {children}
    </DarkModeContext.Provider>
  );
}

export function useDarkMode() {
  const context = useContext(DarkModeContext);
  if (!context) {
    throw new Error('useDarkMode must be used within a DarkModeProvider');
  }
  return context;
}
