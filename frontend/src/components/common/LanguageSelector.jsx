import { useState, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';

// Pages whose header already provides a language control, so the floating button stays off there:
// the partner layout (docks this component inline) and the home page (Navbar's own language select)
const DOCKED_ROUTE = /^\/$|^\/partner(\/|$)/;

/**
 * placement="floating" (default): the global bottom-right button rendered once in App.
 * placement="inline": a header-sized button with a drop-down, for layouts listed in DOCKED_ROUTE.
 */
export default function LanguageSelector({ placement = 'floating' }) {
  const inline = placement === 'inline';
  const { language, setLanguage, LANGUAGES } = useLanguage();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const location = useLocation();

  const isAuthRoute =
    location.pathname.startsWith('/login') ||
    location.pathname.startsWith('/register') ||
    location.pathname.startsWith('/operator/login') ||
    location.pathname.startsWith('/operator/auth') ||
    location.pathname.startsWith('/auth/operator') ||
    location.pathname.startsWith('/verify-otp') ||
    location.pathname.startsWith('/verify-email') ||
    location.pathname.startsWith('/forgot-password') ||
    location.pathname.startsWith('/reset-password');

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (isAuthRoute || (!inline && DOCKED_ROUTE.test(location.pathname))) {
    return null;
  }

  const currentLangObj = LANGUAGES.find((l) => l.code === language) || LANGUAGES[0];

  return (
    // Floating z-40: above sticky page headers (z-30) but below modals (z-50) and the mobile nav drawer (z-[55]+)
    <div
      className={inline ? 'relative font-sans' : 'fixed bottom-4 right-4 sm:bottom-6 sm:right-6 z-40 font-sans'}
      ref={dropdownRef}
    >
      {isOpen && (
        <div
          className={`${
            inline ? 'absolute right-0 top-full mt-2 z-50' : 'mb-3'
          } w-64 max-w-[calc(100vw-2rem)] bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl border border-emerald-900/10 p-2 space-y-1 animate-in fade-in slide-in-from-bottom-3 duration-200`}
        >
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
                      ? 'bg-emerald-700 text-white shadow-md'
                      : 'text-slate-700 hover:bg-emerald-50 hover:text-emerald-700'
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <span>{lang.native}</span>
                    <span className={`text-[10px] font-normal ${isActive ? 'text-emerald-100' : 'text-slate-400'}`}>
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
        className={
          inline
            ? 'p-2.5 sm:px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-700 transition-colors flex items-center gap-2 group'
            : 'p-3 sm:px-4 bg-emerald-700 hover:bg-emerald-800 text-white rounded-full shadow-xl hover:shadow-2xl transition-all duration-200 flex items-center gap-2.5 border-2 border-white/20 active:scale-95 group'
        }
        title="Change Regional Language"
        aria-label={`Change language (current: ${currentLangObj.label})`}
        aria-expanded={isOpen}
      >
        <span aria-hidden="true" className="text-lg leading-none group-hover:rotate-45 transition-transform duration-300">🌐</span>
        {/* Globe-only on phones (floating: covers less of the page) and below xl when docked (header space) */}
        <span className={`hidden ${inline ? 'xl:inline' : 'sm:inline'} text-xs font-extrabold tracking-wide uppercase`}>
          {currentLangObj.native}
        </span>
        <span aria-hidden="true" className={`hidden ${inline ? 'xl:inline' : 'sm:inline'} text-[10px] opacity-80`}>
          {isOpen ? '▲' : '▼'}
        </span>
      </button>
    </div>
  );
}
