import { useState, useRef, useEffect } from 'react';
import { useLanguage } from '../../context/LanguageContext';

export default function LanguageSelector() {
  const { language, setLanguage, LANGUAGES } = useLanguage();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentLangObj = LANGUAGES.find((l) => l.code === language) || LANGUAGES[0];

  return (
    <div className="fixed bottom-6 right-6 z-50 font-sans" ref={dropdownRef}>
      {isOpen && (
        <div className="mb-3 w-64 bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl border border-emerald-900/10 p-2 space-y-1 animate-in fade-in slide-in-from-bottom-3 duration-200">
          <div className="px-3 py-2 border-b border-gray-100 flex items-center justify-between">
            <span className="text-xs font-extrabold uppercase tracking-wider text-emerald-800 flex items-center gap-1.5">
              <span>🌐</span> Select Language
            </span>
            <button
              onClick={() => setIsOpen(false)}
              className="text-gray-400 hover:text-gray-600 text-xs font-bold"
            >
              ✕
            </button>
          </div>
          <div className="max-h-60 overflow-y-auto custom-scrollbar space-y-0.5">
            {LANGUAGES.map((lang) => {
              const isActive = lang.code === language;
              return (
                <button
                  key={lang.code}
                  onClick={() => {
                    setLanguage(lang.code);
                    setIsOpen(false);
                  }}
                  className={`w-full px-3 py-2 rounded-xl text-left text-xs font-bold flex items-center justify-between transition-all ${
                    isActive
                      ? 'bg-[#3E7B27] text-white shadow-md'
                      : 'text-gray-700 hover:bg-emerald-50 hover:text-[#3E7B27]'
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <span>{lang.native}</span>
                    <span className={`text-[10px] font-normal ${isActive ? 'text-emerald-100' : 'text-gray-400'}`}>
                      ({lang.label})
                    </span>
                  </span>
                  {isActive && <span className="text-xs">✓</span>}
                </button>
              );
            })}
          </div>
        </div>
      )}

      <button
        onClick={() => setIsOpen(!isOpen)}
        className="px-4 py-3 bg-[#3E7B27] hover:bg-[#2E6F22] text-white rounded-full shadow-xl hover:shadow-2xl transition-all duration-200 flex items-center gap-2.5 border-2 border-white/20 active:scale-95 group"
        title="Change Regional Language"
      >
        <span className="text-lg group-hover:rotate-45 transition-transform duration-300">🌐</span>
        <span className="text-xs font-extrabold tracking-wide uppercase">{currentLangObj.native}</span>
        <span className="text-[10px] opacity-80">{isOpen ? '▲' : '▼'}</span>
      </button>
    </div>
  );
}
