import { Link, Outlet, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import heroGoldenHour from "../assets/images/hero-golden-hour.jpg";

const EASE = [0.22, 1, 0.36, 1];

function AuthLayout() {
  const location = useLocation();

  return (
    <div className="flex min-h-screen flex-col bg-white lg:flex-row">
      <div className="relative h-[220px] w-full shrink-0 overflow-hidden sm:h-[300px] lg:h-auto lg:w-[44%] xl:w-[48%]">
        <img
          src={heroGoldenHour}
          alt="Golden-hour view of Indian farmland"
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-emerald-950/95 via-emerald-950/35 to-emerald-950/10 lg:via-emerald-950/25" />
        <div className="absolute inset-0 hidden bg-gradient-to-r from-transparent via-transparent to-emerald-950/20 lg:block" />

        <Link to="/" className="absolute left-6 top-6 z-10 flex items-center gap-2 sm:left-8 sm:top-8">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-lime-400 text-base font-bold text-emerald-950 shadow-lg">
            🌱
          </span>
          <span className="font-display text-lg font-bold text-white drop-shadow-md">AgroRent</span>
        </Link>

        <div className="absolute inset-x-0 bottom-0 z-10 p-6 sm:p-8 lg:p-12 xl:p-16">
          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: EASE }}
            className="font-display text-2xl font-bold leading-[1.15] text-white [text-shadow:0_0_35px_rgba(163,230,53,0.45)] sm:text-3xl lg:text-4xl xl:text-[2.65rem]"
          >
            Empowering India&apos;s Farms,
            <br />
            One Acre at a Time.
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.15, ease: EASE }}
            className="mt-4 hidden max-w-sm text-sm leading-relaxed text-white/70 lg:block"
          >
            Trusted by 50,000+ farmers for equipment rentals, verified operators, and
            same-day harvest support — anywhere in rural India.
          </motion.p>
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
