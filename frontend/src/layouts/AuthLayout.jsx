import { useState, useRef, useEffect } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { Globe, ChevronDown, Check } from 'lucide-react';
import heroGoldenHour from '../assets/images/hero-golden-hour.jpg';
import farmerImage from '../assets/images/ModuleService Images/Farmer.jpeg';
import equipmentOwnerImage from '../assets/images/ModuleService Images/Equipment Owner.jpeg';
import operatorImage from '../assets/images/ModuleService Images/Operator.jpeg';
import agroRentLogo from '../assets/images/agrorent-logo.jpeg';
import { useLanguage } from '../context/LanguageContext';

const EASE = [0.22, 1, 0.36, 1];

const HERO_CONTENT = {
  '/register': {
    image: farmerImage,
    alt: 'Farmer using AgroRent in the field',
    headline: (
      <>
        Book Any Farm Service,
        <br />
        In Minutes.
      </>
    ),
    subtext:
      'Join 50,000+ farmers already renting equipment, booking operators, and tracking every job live on AgroRent.',
  },
  '/register/partner': {
    image: equipmentOwnerImage,
    alt: 'Equipment owner with a tractor fleet',
    headline: (
      <>
        Turn Your Machines
        <br />
        Into Income.
      </>
    ),
    subtext:
      '3,200+ machines are already listed on AgroRent — list yours, accept bookings instantly, and get paid on time.',
  },
  '/register/operator': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Get Matched to Jobs,
        <br />
        Near You.
      </>
    ),
    subtext:
      'Join 12,000+ verified operators earning steady income with live job alerts and guaranteed payouts.',
  },
  '/login/operator': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Field Machinery,
        <br />
        Under Your Control.
      </>
    ),
    subtext:
      'Access your assigned farm machinery tasks, live GPS check-ins, and daily payout records.',
  },
  '/operator/login': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Field Machinery,
        <br />
        Under Your Control.
      </>
    ),
    subtext:
      'Access your assigned farm machinery tasks, live GPS check-ins, and daily payout records.',
  },
  '/auth/operator': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Get Matched to Jobs,
        <br />
        Near You.
      </>
    ),
    subtext:
      'Join 12,000+ verified operators earning steady income with live job alerts and fast payouts.',
  },
  '/verify-otp/operator': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Secure Identity,
        <br />
        Instant Verification.
      </>
    ),
    subtext:
      'Verify your phone number with instant OTP to secure your machinery operations profile.',
  },
  '/register/operator/kyc': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Certified Skills,
        <br />
        Guaranteed Payouts.
      </>
    ),
    subtext:
      'Upload your driving license and Aadhaar credentials for fast administrative review.',
  },
  '/register/operator/pending': {
    image: operatorImage,
    alt: 'Verified AgroRent equipment operator',
    headline: (
      <>
        Application Received,
        <br />
        Review in Progress.
      </>
    ),
    subtext:
      'Your operator credentials are being reviewed by fleet administrators. You will be notified once active.',
  },
};

const DEFAULT_HERO = {
  image: heroGoldenHour,
  alt: 'Golden-hour view of Indian farmland',
  headline: (
    <>
      Empowering India&apos;s Farms,
      <br />
      One Acre at a Time.
    </>
  ),
  subtext:
    'Trusted by 50,000+ farmers for equipment rentals, verified operators, and same-day harvest support — anywhere in rural India.',
};

function AuthLanguageDropdown() {
  const { language, setLanguage, LANGUAGES } = useLanguage();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentLang = LANGUAGES.find((l) => l.code === language) || LANGUAGES[0];

  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className="flex items-center gap-2 rounded-xl border border-slate-200/90 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm transition-all duration-200 hover:border-emerald-300 hover:bg-emerald-50/40 focus:outline-none focus:ring-2 focus:ring-emerald-600/20"
      >
        <Globe className="h-3.5 w-3.5 text-emerald-700" aria-hidden="true" />
        <span>{currentLang.native}</span>
        <ChevronDown
          className={`h-3 w-3 text-slate-400 transition-transform duration-200 ${
            isOpen ? 'rotate-180' : ''
          }`}
          aria-hidden="true"
        />
      </button>

      {isOpen && (
        <div
          role="listbox"
          className="absolute right-0 top-full z-50 mt-1.5 w-48 rounded-2xl border border-slate-200/90 bg-white p-1.5 shadow-xl ring-1 ring-slate-900/5 transition-all animate-in fade-in zoom-in-95 duration-150"
        >
          <div className="max-h-56 overflow-y-auto custom-scrollbar space-y-0.5">
            {LANGUAGES.map((lang) => {
              const isSelected = lang.code === language;
              return (
                <button
                  key={lang.code}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => {
                    setLanguage(lang.code);
                    setIsOpen(false);
                  }}
                  className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-semibold transition-colors ${
                    isSelected
                      ? 'bg-emerald-50 text-emerald-900 font-bold'
                      : 'text-slate-700 hover:bg-slate-50 hover:text-emerald-800'
                  }`}
                >
                  <span className="flex items-center gap-1.5">
                    <span>{lang.native}</span>
                    <span className="text-[10px] font-normal text-slate-400">
                      ({lang.label})
                    </span>
                  </span>
                  {isSelected && (
                    <Check className="h-3.5 w-3.5 text-emerald-700" aria-hidden="true" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function AuthLayout() {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const searchRole = searchParams.get('role')?.toLowerCase();

  let key = location.pathname;
  if (location.pathname === '/register' && searchRole) {
    const norm = searchRole === 'owner' ? 'partner' : searchRole;
    key = `/register/${norm}`;
  } else if (location.pathname === '/login' && searchRole === 'operator') {
    key = '/login/operator';
  }

  const hero = HERO_CONTENT[key] || HERO_CONTENT[location.pathname] || DEFAULT_HERO;

  return (
    <div className="flex min-h-screen flex-col bg-white lg:flex-row">
      {/* Left Photographic Panel */}
      <div className="relative h-[220px] w-full shrink-0 overflow-hidden sm:h-[280px] lg:sticky lg:top-0 lg:h-screen lg:w-[46%] xl:w-[48%]">
        <AnimatePresence mode="wait">
          <motion.img
            key={hero.image}
            src={hero.image}
            alt={hero.alt}
            initial={{ opacity: 0, scale: 1.04 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.5, ease: EASE }}
            className="absolute inset-0 h-full w-full object-cover"
          />
        </AnimatePresence>

        {/* Cohesive Dark Green Gradient Overlays */}
        <div className="absolute inset-0 bg-gradient-to-t from-emerald-950/95 via-emerald-950/45 to-emerald-950/20 lg:via-emerald-950/35" />
        <div className="absolute inset-0 hidden bg-gradient-to-r from-transparent via-transparent to-emerald-950/30 lg:block" />

        {/* AgroRent Brand Logo */}
        <Link
          to="/"
          className="absolute left-6 top-6 z-20 flex items-center gap-2 sm:left-8 sm:top-8"
          aria-label="AgroRent Home"
        >
          <div className="flex h-11 items-center overflow-hidden rounded-xl bg-white px-3 py-1 shadow-lg ring-1 ring-slate-900/10 transition-transform duration-200 hover:scale-[1.02]">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
        </Link>

        {/* Hero Promotional Typography Area */}
        <div className="absolute inset-x-0 bottom-0 z-10 p-6 sm:p-8 lg:p-12 xl:p-16">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.5, ease: EASE }}
            >
              <h2 className="font-display text-2xl font-bold leading-[1.18] text-white [text-shadow:0_0_35px_rgba(163,230,53,0.45)] sm:text-3xl lg:text-4xl xl:text-[2.65rem]">
                {hero.headline}
              </h2>

              <p className="mt-4 hidden max-w-sm text-sm leading-relaxed text-white/80 lg:block">
                {hero.subtext}
              </p>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>

      {/* Right Authentication Panel */}
      <div className="flex flex-1 flex-col min-h-screen bg-slate-50/40 lg:bg-white overflow-y-auto">
        {/* Top Header with Language Selector */}
        <div className="flex w-full items-center justify-end px-6 pt-6 sm:px-8">
          <AuthLanguageDropdown />
        </div>

        {/* Centered Form Shell with Unified Width */}
        <div className="mx-auto flex w-full max-w-[480px] flex-1 flex-col justify-center px-6 py-6 pb-16 sm:px-8 sm:py-10">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.3, ease: EASE }}
              className="w-full"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

export default AuthLayout;