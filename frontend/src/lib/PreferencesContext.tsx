import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

export type Preferences = {
  boardColor: 'BROWN' | 'BLUE' | 'GREEN';
  animationType: 'NORMAL' | 'FUN';
}

type PreferencesContextType = {
  preferences: Preferences;
  updatePreferences: (newPrefs: Partial<Preferences>) => void;
}

const PreferencesContext = createContext<PreferencesContextType | undefined>(undefined);

export function PreferencesProvider({ children }: { children: ReactNode }) {
  const [preferences, setPreferences] = useState<Preferences>(() => {
    // Load from localStorage. Default to brown board and normal animations if nothing stored.
    const saved = localStorage.getItem('chessPreferences');
    return saved ? JSON.parse(saved) : {
      boardColor: 'BROWN',
      animationType: 'NORMAL'
    };
  });

  // Save to localStorage whenever preferences change.
  useEffect(() => {
    localStorage.setItem('chessPreferences', JSON.stringify(preferences));
  }, [preferences]);

  const updatePreferences = (newPrefs: Partial<Preferences>) => {
    setPreferences(prev => ({ ...prev, ...newPrefs }));
  };

  return (
    <PreferencesContext.Provider value={{ preferences, updatePreferences }}>
      {children}
    </PreferencesContext.Provider>
  );
}

export function usePreferences() {
  const context = useContext(PreferencesContext);
  if (!context) throw new Error('usePreferences must be used within PreferencesProvider');
  return context;
}