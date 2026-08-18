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
import MagneticButton from "../ui/MagneticButton";
import { RevealGroup, RevealItem, RevealText } from "../motion/Reveal";

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

      <div className="relative mx-auto grid max-w-7xl items-center gap-10 px-4 py-16 sm:px-6 sm:py-20 md:gap-12 lg:grid-cols-[1.1fr_0.9fr] lg:gap-14 lg:py-28 xl:py-32">
        {/* Headline — set directly on the image, no glass box */}
        <RevealGroup stagger={0.12} delayChildren={0.1} amount={0.1}>
          <RevealItem>
            <span className="inline-flex items-center gap-2 rounded-full border border-lime-300/25 bg-emerald-950/40 px-3 py-1.5 text-xs font-medium text-lime-200 backdrop-blur-md sm:px-4 sm:text-sm">
              <Zap
                className="h-4 w-4 shrink-0 text-lime-300"
                fill="currentColor"
              />
              India&apos;s Agriculture Service Marketplace
            </span>
          </RevealItem>

          <h1 className="mt-5 max-w-2xl font-display text-4xl font-bold leading-[1.1] tracking-tight text-white drop-shadow-[0_2px_20px_rgba(0,0,0,0.5)] sm:mt-7 sm:text-5xl md:text-6xl lg:leading-[1.05] lg:text-7xl">
            <RevealText text="Book Farm Equipment & Operators" delay={0.15} />
            <RevealText
              text="Any Crop, Any Acre."
              delay={0.5}
              className="mt-1.5 block bg-gradient-to-r from-lime-300 via-lime-200 to-emerald-300 bg-clip-text text-transparent sm:mt-2"
            />
          </h1>

          <RevealItem>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-white/80 drop-shadow-[0_1px_10px_rgba(0,0,0,0.6)] sm:mt-7 sm:text-lg">
              Connects farmers with nearby tractors, harvesters, drones, and
              skilled operators. Acre-based pricing, live GPS tracking,
              WhatsApp booking — built for rural India.
            </p>
          </RevealItem>

          <RevealItem>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:gap-4 lg:mt-10">
              <MagneticButton className="w-full sm:w-auto">
                <span
                  aria-hidden="true"
                  className="absolute -inset-1 animate-pulse rounded-xl bg-lime-400/40 blur-lg"
                />
                <Link
                  to="/register"
                  className="group relative inline-flex min-h-[52px] w-full items-center justify-center gap-2 rounded-xl bg-lime-400 px-8 py-4 font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.5)] transition-[background-color,box-shadow] duration-300 ease-out hover:bg-lime-300 hover:shadow-[0_0_35px_rgba(132,204,22,0.8)] sm:w-auto"
                >
                  Get Started
                  <ArrowRight className="h-5 w-5 transition-transform duration-300 ease-out group-hover:translate-x-1" />
                </Link>
              </MagneticButton>

              <Link
                to="/login"
                className="inline-flex min-h-[52px] w-full items-center justify-center gap-2 rounded-xl border border-white/25 bg-white/5 px-8 py-4 font-semibold text-white backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-1 hover:border-white/40 hover:bg-white/15 sm:w-auto"
              >
                Log In
              </Link>
            </div>
          </RevealItem>

          <RevealItem>
            <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-3 sm:flex sm:flex-wrap sm:gap-x-7 lg:mt-12">
              {trustBadges.map(({ icon: Icon, label }) => (
                <div
                  key={label}
                  className="flex items-center gap-2 text-xs text-white/75 transition-colors duration-300 ease-out hover:text-white sm:text-sm"
                >
                  <Icon className="h-4 w-4 shrink-0 text-lime-300" />
                  {label}
                </div>
              ))}
            </div>
          </RevealItem>
        </RevealGroup>

        {/* Role selector — dark translucent so it stays crisp over the photo */}
        <div className="w-full">
          <RevealItem>
            <p className="mb-3 text-[11px] font-semibold uppercase tracking-[0.2em] text-white/50 sm:mb-4 sm:text-xs">
              Choose your role
            </p>
          </RevealItem>
          <RevealGroup
            className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-1"
            stagger={0.1}
            delayChildren={0.35}
            amount={0.1}
          >
            {roleCards.map(({ to, icon: Icon, iconBg, title, description }) => (
              <RevealItem key={title} direction="left">
                <Link
                  to={to}
                  className="group flex min-h-[44px] items-center gap-3 rounded-2xl border border-white/10 bg-emerald-950/55 p-3.5 shadow-xl shadow-black/30 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-1 hover:border-lime-300/50 hover:bg-emerald-900/70 hover:shadow-2xl hover:shadow-lime-400/15 sm:gap-4 sm:p-4"
                >
                  <span
                    className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${iconBg} shadow-lg transition-all duration-300 ease-out group-hover:scale-110`}
                  >
                    <Icon className="h-5 w-5 text-white" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-semibold text-white sm:text-base">
                      {title}
                    </span>
                    <span className="block text-xs text-white/60 sm:text-sm">
                      {description}
                    </span>
                  </span>
                  <ArrowRight className="h-5 w-5 shrink-0 text-white/30 transition-all duration-300 ease-out group-hover:translate-x-1 group-hover:text-lime-300" />
                </Link>
              </RevealItem>
            ))}
          </RevealGroup>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
