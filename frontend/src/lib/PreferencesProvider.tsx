import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

/**
 * Closely related to the PlayerPreferences type.
 * This type is more restrictive to reflect the actual options users choose from.
 */
export type Preferences = {
  boardColour: 'BROWN' | 'BLUE' | 'GREEN';
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
    const defaultPreferences: Preferences = {
        boardColour: 'BROWN',
        animationType: 'NORMAL'
    };
    const loadedPreferences: Preferences | null = saved && JSON.parse(saved);
    return loadedPreferences?.boardColour ? loadedPreferences : defaultPreferences; // In case the existing preferences uses the old spelling of "boardColor" (without a u)
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