import { createContext, useContext, useState, useEffect } from 'react';
import { translations, LANGUAGES } from '../i18n/translations';

const LanguageContext = createContext();

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(() => {
    return localStorage.getItem('agro_language') || 'English';
  });

  useEffect(() => {
    localStorage.setItem('agro_language', language);
  }, [language]);

  const t = (key) => {
    if (!translations[key]) return key;
    return translations[key][language] || translations[key]['English'] || key;
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t, LANGUAGES }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
}
