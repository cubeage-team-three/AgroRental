import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Globe, Menu, X } from 'lucide-react';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';

const navLinks = [
  { label: 'Services', href: '/#services' },
  { label: 'How It Works', href: '/#how-it-works' },
  { label: 'Panels', href: '/#panels' },
  { label: 'Pricing', href: '/#pricing' },
];

function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const auth = useAuth();

  let t = (key, defaultVal) => defaultVal || key;
  let language = 'English';
  let setLanguage = () => { };
  let LANGUAGES = [];

  try {
    const langCtx = useLanguage();
    if (langCtx) {
      t = (key, defaultVal) => langCtx.t(key) !== key ? langCtx.t(key) : (defaultVal || key);
      language = langCtx.language;
      setLanguage = langCtx.setLanguage;
      LANGUAGES = langCtx.LANGUAGES || [];
    }
  } catch (e) {
    // Fallback if not wrapped in LanguageProvider
  }

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 20) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const getDashboardLink = () => {
    if (auth.role === 'ADMIN') return '/admin/dashboard';
    if (auth.role === 'PARTNER') return '/partner/dashboard';
    if (auth.role === 'OPERATOR') return '/operator/dashboard';
    return '/farmer/dashboard';
  };

  return (
    <header
      className={`sticky top-0 z-50 transition-all duration-300 ease-out ${isScrolled
          ? 'border-b border-slate-200/60 bg-white/75 shadow-lg shadow-emerald-900/5 backdrop-blur-xl'
          : 'border-b border-transparent bg-white/50 backdrop-blur-md'
        }`}
    >
      <nav
        className={`mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 transition-all duration-300 ease-out sm:px-6 ${isScrolled ? "py-1.5" : "py-3"
          }`}
      >
        <Link
          to="/"
          className="flex shrink-0 items-center transition-transform duration-300 ease-out hover:scale-105"
          onClick={() => setIsOpen(false)}
        >
          {/*
            The logo is a JPEG (no alpha) on a white background with generous
            baked-in padding. The wrapper crops that dead space so the mark reads
            larger, and the white card makes the opaque background deliberate
            rather than a stray rectangle floating on the glass bar.
            Swap in a transparent PNG/SVG and this card can go away.
          */}
          <span
            className={`flex items-center overflow-hidden rounded-xl bg-white px-2.5 shadow-sm ring-1 ring-slate-900/5 transition-all duration-300 ease-out ${isScrolled ? "h-10 sm:h-11" : "h-12 sm:h-14"
              }`}
          >
            <img
              src={agroRentLogo}
              alt="AgroRent — Agriculture Service Marketplace"
              className="h-[175%] w-auto max-w-none object-contain"
            />
          </span>
        </Link>

        {/* Pill nav */}
        <div className="hidden items-center gap-1 rounded-full border border-slate-200/70 bg-slate-100/60 p-1.5 backdrop-blur-md lg:flex">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="inline-flex min-h-[44px] items-center rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition-all duration-300 ease-out hover:bg-white hover:text-emerald-700 hover:shadow-sm"
            >
              {t(link.label.toLowerCase().replace(/ /g, '_'), link.label)}
            </a>
          ))}
        </div>

        {/* Actions */}
        <div className="hidden shrink-0 items-center gap-3 md:flex">
          {Array.isArray(LANGUAGES) && LANGUAGES.length > 0 && (
            <div className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white/70 px-3 py-2 text-xs font-semibold text-slate-700 backdrop-blur-md">
              <Globe className="h-4 w-4 text-emerald-600" />
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                className="bg-transparent font-medium focus:outline-none cursor-pointer"
              >
                {LANGUAGES.map((lang) => {
                  const code = typeof lang === 'string' ? lang : (lang.code || lang.label);
                  const label = typeof lang === 'string' ? lang : (lang.native || lang.label);
                  return (
                    <option key={code} value={code}>
                      {label}
                    </option>
                  );
                })}
              </select>
            </div>
          )}
          {auth.isAuthenticated ? (
            <div className="flex items-center gap-2">
              <Link
                to={getDashboardLink()}
                className="inline-flex min-h-[44px] items-center rounded-xl bg-emerald-700 px-4 py-2 text-xs font-bold text-white shadow-md transition-all duration-300 hover:bg-emerald-800"
              >
                Dashboard
              </Link>
              <button
                onClick={() => auth.logout()}
                className="inline-flex min-h-[44px] items-center rounded-xl border border-red-200 bg-red-50 px-3.5 py-2 text-xs font-bold text-red-700 hover:bg-red-100"
              >
                Logout
              </button>
            </div>
          ) : (
            <>
              <Link
                to="/login"
                className="inline-flex min-h-[44px] items-center rounded-xl border border-slate-200 bg-white/70 px-5 py-2.5 text-sm font-semibold text-slate-700 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-emerald-300 hover:text-emerald-800 hover:shadow-md"
              >
                {t('login', 'Log In')}
              </Link>
              <Link
                to="/register"
                className="group inline-flex min-h-[44px] items-center gap-1.5 rounded-xl bg-lime-400 px-5 py-2.5 text-sm font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.45)] transition-all duration-300 ease-out hover:-translate-y-0.5 hover:bg-lime-300 hover:shadow-[0_0_25px_rgba(132,204,22,0.7)]"
              >
                {t('register', 'Sign Up Free')}
                <ArrowRight className="h-4 w-4 transition-transform duration-300 ease-out group-hover:translate-x-1" />
              </Link>
            </>
          )}
        </div>

        {/* Mobile toggle */}
        <button
          type="button"
          onClick={() => setIsOpen((open) => !open)}
          aria-label={isOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={isOpen}
          className="flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white/70 text-slate-700 backdrop-blur-md transition-all duration-300 ease-out hover:border-emerald-300 hover:text-emerald-700 md:hidden"
        >
          {isOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </nav>

      {/* Mobile panel */}
      {isOpen && (
        <div className="border-t border-slate-200/60 bg-white/90 backdrop-blur-xl md:hidden">
          <div className="mx-auto max-w-7xl space-y-1 px-6 py-4">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                onClick={() => setIsOpen(false)}
                className="block rounded-xl px-4 py-3 text-sm font-medium text-slate-600 transition-all duration-300 ease-out hover:bg-emerald-50 hover:text-emerald-800"
              >
                {t(link.label.toLowerCase().replace(/ /g, '_'), link.label)}
              </a>
            ))}
            <div className="flex flex-col gap-2 pt-3">
              <Link
                to="/login"
                onClick={() => setIsOpen(false)}
                className="rounded-xl border border-slate-200 px-4 py-3 text-center text-sm font-semibold text-slate-700 transition-all duration-300 ease-out hover:border-emerald-300 hover:text-emerald-800"
              >
                {t('login', 'Log In')}
              </Link>
              <Link
                to="/register"
                onClick={() => setIsOpen(false)}
                className="rounded-xl bg-lime-400 px-4 py-3 text-center text-sm font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.45)] transition-all duration-300 ease-out hover:bg-lime-300"
              >
                {t('register', 'Sign Up Free')}
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

export default Navbar;