import { useRef } from "react";
import { Link } from "react-router-dom";
import { motion, useScroll, useTransform } from "framer-motion";
import {
  Zap,
  ArrowRight,
  Wheat,
  Users,
  Tractor,
  BarChart3,
  CheckCircle2,
  Navigation,
  Globe2,
  ShieldCheck,
} from "lucide-react";
import heroImage from "../../assets/images/hero-golden-hour.jpg";

const roleCards = [
  {
    to: "/register/farmer",
    icon: Wheat,
    iconBg: "bg-emerald-500",
    title: "I'm a Farmer",
    description: "Book equipment, track operators, manage fields",
  },
  {
    to: "/register/operator",
    icon: Users,
    iconBg: "bg-teal-500",
    title: "I'm an Operator",
    description: "Accept jobs, navigate to fields, track earnings",
  },
  {
    to: "/register/partner",
    icon: Tractor,
    iconBg: "bg-amber-500",
    title: "I Own Equipment",
    description: "Manage machines, accept bookings, track earnings",
  },
  {
    to: "/login",
    icon: BarChart3,
    iconBg: "bg-sky-500",
    title: "Platform Admin",
    description: "Operations, analytics, approvals, and control",
  },
];

const trustBadges = [
  { icon: ShieldCheck, label: "Verified Operators" },
  { icon: Navigation, label: "Live GPS Tracking" },
  { icon: Globe2, label: "9 Languages" },
  { icon: CheckCircle2, label: "Secure Payments" },
];

function HeroSection() {
  const sectionRef = useRef(null);
  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start start", "end start"],
  });
  const backgroundY = useTransform(scrollYProgress, [0, 1], ["-4%", "14%"]);

  return (
    <section
      ref={sectionRef}
      className="relative isolate overflow-hidden bg-emerald-950"
    >
      {/* Cinematic parallax backdrop — kept at full saturation */}
      <div className="absolute -inset-y-24 inset-x-0 overflow-hidden">
        <motion.img
          src={heroImage}
          alt="Tractors, harvesters, and spray drones working a field at golden hour"
          style={{ y: backgroundY }}
          className="h-full w-full scale-110 object-cover"
        />
      </div>

      {/* Directional grading: dark where the text sits, clear where the photo shines */}
      <div className="absolute inset-0 bg-gradient-to-r from-emerald-950 via-emerald-950/80 to-emerald-950/20" />
      <div className="absolute inset-0 bg-gradient-to-t from-emerald-950 via-transparent to-emerald-950/40" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_75%_35%,rgba(217,164,65,0.22),transparent_50%)]" />

      <div className="relative mx-auto grid max-w-7xl items-center gap-14 px-6 py-24 lg:grid-cols-[1.1fr_0.9fr] lg:py-32">
        {/* Headline — set directly on the image, no glass box */}
        <div>
          <span className="inline-flex items-center gap-2 rounded-full border border-lime-300/25 bg-emerald-950/40 px-4 py-1.5 text-sm font-medium text-lime-200 backdrop-blur-md">
            <Zap className="h-4 w-4 text-lime-300" fill="currentColor" />
            India&apos;s Agriculture Service Marketplace
          </span>

          <h1 className="mt-7 max-w-2xl font-display text-5xl font-bold leading-[1.05] tracking-tight text-white drop-shadow-[0_2px_20px_rgba(0,0,0,0.5)] sm:text-6xl lg:text-7xl">
            Book Farm Equipment &amp; Operators
            <span className="mt-2 block bg-gradient-to-r from-lime-300 via-lime-200 to-emerald-300 bg-clip-text text-transparent">
              Any Crop, Any Acre.
            </span>
          </h1>

          <p className="mt-7 max-w-xl text-lg leading-relaxed text-white/80 drop-shadow-[0_1px_10px_rgba(0,0,0,0.6)]">
            Connects farmers with nearby tractors, harvesters, drones, and
            skilled operators. Acre-based pricing, live GPS tracking,
            WhatsApp booking — built for rural India.
          </p>

          <div className="mt-10 flex flex-wrap items-center gap-4">
            <div className="relative">
              <span
                aria-hidden="true"
                className="absolute -inset-1 animate-pulse rounded-xl bg-lime-400/40 blur-lg"
              />
              <Link
                to="/register"
                className="group relative inline-flex items-center gap-2 rounded-xl bg-lime-400 px-8 py-4 font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.5)] transition-all duration-300 ease-out hover:-translate-y-1 hover:bg-lime-300 hover:shadow-[0_0_30px_rgba(132,204,22,0.75)]"
              >
                Get Started
                <ArrowRight className="h-5 w-5 transition-transform duration-300 ease-out group-hover:translate-x-1" />
              </Link>
            </div>

            <Link
              to="/login"
              className="inline-flex items-center gap-2 rounded-xl border border-white/25 bg-white/5 px-8 py-4 font-semibold text-white backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-1 hover:border-white/40 hover:bg-white/15"
            >
              Log In
            </Link>
          </div>

          <div className="mt-12 flex flex-wrap gap-x-7 gap-y-3">
            {trustBadges.map(({ icon: Icon, label }) => (
              <div
                key={label}
                className="flex items-center gap-2 text-sm text-white/75 transition-colors duration-300 ease-out hover:text-white"
              >
                <Icon className="h-4 w-4 text-lime-300" />
                {label}
              </div>
            ))}
          </div>
        </div>

        {/* Role selector — dark translucent so it stays crisp over the photo */}
        <div className="w-full">
          <p className="mb-4 text-xs font-semibold uppercase tracking-[0.2em] text-white/50">
            Choose your role
          </p>
          <div className="flex flex-col gap-3">
            {roleCards.map(({ to, icon: Icon, iconBg, title, description }) => (
              <Link
                key={title}
                to={to}
                className="group flex items-center gap-4 rounded-2xl border border-white/10 bg-emerald-950/55 p-4 shadow-xl shadow-black/30 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-1 hover:border-lime-300/50 hover:bg-emerald-900/70 hover:shadow-2xl hover:shadow-lime-400/15"
              >
                <span
                  className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${iconBg} shadow-lg transition-all duration-300 ease-out group-hover:scale-110`}
                >
                  <Icon className="h-5 w-5 text-white" />
                </span>
                <span className="flex-1">
                  <span className="block font-semibold text-white">
                    {title}
                  </span>
                  <span className="block text-sm text-white/60">
                    {description}
                  </span>
                </span>
                <ArrowRight className="h-5 w-5 shrink-0 text-white/30 transition-all duration-300 ease-out group-hover:translate-x-1 group-hover:text-lime-300" />
              </Link>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
