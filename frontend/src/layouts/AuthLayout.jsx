import { Link, Outlet, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import heroGoldenHour from "../assets/images/hero-golden-hour.jpg";
import farmerImage from "../assets/images/ModuleService Images/Farmer.jpeg";
import equipmentOwnerImage from "../assets/images/ModuleService Images/Equipment Owner.jpeg";
import operatorImage from "../assets/images/ModuleService Images/Operator.jpeg";
import agroRentLogo from "../assets/images/agrorent-logo.jpeg";

const EASE = [0.22, 1, 0.36, 1];

const HERO_CONTENT = {
  "/register": {
    image: farmerImage,
    alt: "Farmer using AgroRent in the field",
    headline: (
      <>
        Book Any Farm Service,
        <br />
        In Minutes.
      </>
    ),
    subtext:
      "Join 50,000+ farmers already renting equipment, booking operators, and tracking every job live on AgroRent.",
  },
  "/register/partner": {
    image: equipmentOwnerImage,
    alt: "Equipment owner with a tractor fleet",
    headline: (
      <>
        Turn Your Machines
        <br />
        Into Income.
      </>
    ),
    subtext:
      "3,200+ machines are already listed on AgroRent — list yours, accept bookings instantly, and get paid on time.",
  },
  "/register/operator": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Get Matched to Jobs,
        <br />
        Near You.
      </>
    ),
    subtext:
      "Join 12,000+ verified operators earning steady income with live job alerts and fast payouts.",
  },
  "/login/operator": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Welcome Back,
        <br />
        Equipment Operator.
      </>
    ),
    subtext:
      "Sign in to access your deployed field jobs, machinery schedules, and earnings.",
  },
  "/operator/login": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Welcome Back,
        <br />
        Equipment Operator.
      </>
    ),
    subtext:
      "Sign in to access your deployed field jobs, machinery schedules, and earnings.",
  },
  "/login/operator": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Field Machinery,
        <br />
        Under Your Control.
      </>
    ),
    subtext: "Access your assigned farm machinery tasks, live GPS check-ins, and daily payout records.",
  },
  "/operator/login": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Field Machinery,
        <br />
        Under Your Control.
      </>
    ),
    subtext: "Access your assigned farm machinery tasks, live GPS check-ins, and daily payout records.",
  },
  "/auth/operator": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Get Matched to Jobs,
        <br />
        Near You.
      </>
    ),
    subtext: "Join 12,000+ verified operators earning steady income with live job alerts and fast payouts.",
  },
  "/verify-otp/operator": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Secure Identity,
        <br />
        Instant Verification.
      </>
    ),
    subtext: "Verify your phone number with instant OTP to secure your machinery operations profile.",
  },
  "/register/operator/kyc": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Certified Skills,
        <br />
        Guaranteed Payouts.
      </>
    ),
    subtext: "Upload your driving license and Aadhaar credentials for fast administrative review.",
  },
  "/register/operator/pending": {
    image: operatorImage,
    alt: "Verified AgroRent equipment operator",
    headline: (
      <>
        Application Received,
        <br />
        Review in Progress.
      </>
    ),
    subtext: "Your operator credentials are being reviewed by fleet administrators. You will be notified once active.",
  },
};

const DEFAULT_HERO = {
  image: heroGoldenHour,
  alt: "Golden-hour view of Indian farmland",
  headline: (
    <>
      Empowering India&apos;s Farms,
      <br />
      One Acre at a Time.
    </>
  ),
  subtext:
    "Trusted by 50,000+ farmers for equipment rentals, verified operators, and same-day harvest support — anywhere in rural India.",
};

function AuthLayout() {
  const location = useLocation();
  const hero = HERO_CONTENT[location.pathname] || DEFAULT_HERO;

  return (
    <div className="flex min-h-screen flex-col bg-white lg:flex-row">
      <div className="relative h-[220px] w-full shrink-0 overflow-hidden sm:h-[300px] lg:h-auto lg:w-[44%] xl:w-[48%]">
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

        <div className="absolute inset-0 bg-gradient-to-t from-emerald-950/95 via-emerald-950/35 to-emerald-950/10 lg:via-emerald-950/25" />
        <div className="absolute inset-0 hidden bg-gradient-to-r from-transparent via-transparent to-emerald-950/20 lg:block" />

        {/* AgroRent Logo */}
        <Link
          to="/"
          className="absolute left-6 top-6 z-10 flex items-center gap-2 sm:left-8 sm:top-8"
        >
          <div className="flex h-11 items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-lg ring-1 ring-slate-900/10">
            <img
              src={agroRentLogo}
              alt="AgroRent"
              className="h-full w-auto object-contain"
            />
          </div>
        </Link>

        <div className="absolute inset-x-0 bottom-0 z-10 p-6 sm:p-8 lg:p-12 xl:p-16">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.5, ease: EASE }}
            >
              <h2 className="font-display text-2xl font-bold leading-[1.15] text-white [text-shadow:0_0_35px_rgba(163,230,53,0.45)] sm:text-3xl lg:text-4xl xl:text-[2.65rem]">
                {hero.headline}
              </h2>

              <p className="mt-4 hidden max-w-sm text-sm leading-relaxed text-white/70 lg:block">
                {hero.subtext}
              </p>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>

      <div className="flex flex-1 flex-col">
        <div className="mx-auto flex w-full max-w-[420px] flex-1 flex-col justify-center px-6 py-10 pb-16 sm:px-8 sm:py-14">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, x: 24 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -24 }}
              transition={{ duration: 0.35, ease: EASE }}
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