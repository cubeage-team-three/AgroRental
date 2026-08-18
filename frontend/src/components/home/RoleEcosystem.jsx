import { useState } from "react";
import { Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Wheat, Users, Tractor, BarChart3, Check, ArrowRight } from "lucide-react";
import farmerImage from "../../assets/images/ModuleService Images/Farmer.jpeg";
import operatorImage from "../../assets/images/ModuleService Images/Operator.jpeg";
import equipmentOwnerImage from "../../assets/images/ModuleService Images/Equipment Owner.jpeg";
import adminImage from "../../assets/images/ModuleService Images/Admin.jpeg";

const roles = [
  {
    id: "farmer",
    label: "Farmer",
    icon: Wheat,
    title: "Book Any Farm Service in Minutes",
    ctaLabel: "Get Started as Farmer",
    ctaTo: "/register/farmer",
    image: farmerImage,
    stat: { value: "50,000+", label: "Farmers Served" },
    points: [
      "OTP login — no passwords needed",
      "Map-based farm pinning for multiple plots",
      "Browse by category: equipment + operator services",
      "Filters: distance, price, availability, capacity/acre",
      "Acre-based booking with live cost estimate",
      "Live GPS tracking of arriving operators",
      "UPI / Card / Wallet payments with auto invoice",
      "9 regional language support",
    ],
  },
  {
    id: "operator",
    label: "Operator",
    icon: Users,
    title: "Get Matched to Jobs Near You",
    ctaLabel: "Get Started as Operator",
    ctaTo: "/register/operator",
    image: operatorImage,
    stat: { value: "12,000+", label: "Verified Operators" },
    points: [
      "Simple registration with Aadhaar & license verification",
      "Get assigned to jobs by partners or admin",
      "Live GPS check-in when a job starts",
      "Daily and job-based earnings tracking",
      "Job history and performance ratings",
      "Fast payouts after job completion",
      "WhatsApp job alerts and updates",
      "Multi-language app support",
    ],
  },
  {
    id: "equipment-owner",
    label: "Equipment Owner",
    icon: Tractor,
    title: "Turn Your Machines Into Income",
    ctaLabel: "Get Started as Equipment Owner",
    ctaTo: "/register/partner",
    image: equipmentOwnerImage,
    stat: { value: "3,200+", label: "Machines Listed" },
    points: [
      "List unlimited equipment with photos & pricing",
      "Accept or decline booking requests instantly",
      "Assign verified operators to each job",
      "Real-time availability calendar",
      "Track earnings and payout history",
      "GST-ready invoices for every booking",
      "Manage multiple equipment locations",
      "Ratings and reviews from farmers",
    ],
  },
  {
    id: "admin",
    label: "Admin",
    icon: BarChart3,
    title: "Run the Entire Platform from One Dashboard",
    ctaLabel: "Explore Admin Panel",
    ctaTo: "/login",
    image: adminImage,
    stat: { value: "28", label: "States Covered" },
    points: [
      "Approve partner & operator verifications",
      "Live map of active bookings and operators",
      "Region-wise demand heatmaps and analytics",
      "Manage complaints and disputes",
      "Configure pricing rules by season",
      "Full audit logs for every transaction",
      "Payment and payout reconciliation",
      "Role-based access control for staff",
    ],
  },
];

function RoleEcosystem() {
  const [activeId, setActiveId] = useState(roles[0].id);
  const activeRole = roles.find((role) => role.id === activeId);

  return (
    <section
      id="panels"
      className="relative isolate scroll-mt-24 overflow-hidden bg-slate-50"
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_-10%,rgba(16,185,129,0.10),transparent_60%)]" />

      <div className="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:py-24">
        <div className="mx-auto max-w-2xl text-center">
          <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-widest sm:px-4 sm:text-xs text-emerald-700">
            Role-Based Architecture
          </span>
          <h2 className="mt-4 font-display sm:mt-5 text-3xl font-bold tracking-tight text-emerald-950 sm:text-4xl lg:text-5xl">
            Four Panels, One Ecosystem
          </h2>
          <p className="mt-3 text-base leading-relaxed text-slate-600 sm:mt-4 sm:text-lg">
            One platform, four tailored experiences — each built around what
            that role actually needs.
          </p>
        </div>

        {/* Role switcher */}
        <div className="mt-10 flex flex-wrap justify-center gap-3">
          {roles.map((role) => {
            const isActive = role.id === activeId;
            return (
              <button
                key={role.id}
                type="button"
                onClick={() => setActiveId(role.id)}
                aria-pressed={isActive}
                className={`inline-flex min-h-[44px] items-center gap-2 rounded-full px-5 py-3 text-sm font-semibold sm:px-6 transition-all duration-300 ease-out hover:-translate-y-1 ${
                  isActive
                    ? "bg-gradient-to-r from-emerald-700 to-emerald-600 text-white shadow-lg shadow-emerald-600/30"
                    : "border border-slate-200 bg-white/70 text-slate-600 shadow-sm backdrop-blur-md hover:border-emerald-300 hover:text-emerald-800 hover:shadow-md"
                }`}
              >
                <role.icon className="h-4 w-4" />
                {role.label}
              </button>
            );
          })}
        </div>

        {/* Panel */}
        <div className="mt-10 rounded-3xl border border-white/60 bg-white/70 p-5 shadow-2xl shadow-emerald-900/10 backdrop-blur-xl sm:rounded-[2rem] sm:p-8 lg:mt-14 lg:p-10">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeRole.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
              className="grid grid-cols-1 items-center gap-8 md:gap-10 lg:grid-cols-2 lg:gap-14"
            >
              <div>
                <h3 className="font-display text-2xl font-bold tracking-tight text-emerald-950 sm:text-3xl">
                  {activeRole.title}
                </h3>

                <ul className="mt-7 space-y-3">
                  {activeRole.points.map((point) => (
                    <li
                      key={point}
                      className="group flex items-start gap-3 text-slate-700 transition-all duration-300 ease-out hover:translate-x-1 hover:text-emerald-900"
                    >
                      <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 transition-all duration-300 ease-out group-hover:bg-emerald-600 group-hover:text-white">
                        <Check className="h-3 w-3" strokeWidth={3} />
                      </span>
                      <span>{point}</span>
                    </li>
                  ))}
                </ul>

                <Link
                  to={activeRole.ctaTo}
                  className="group mt-8 inline-flex min-h-[52px] w-full items-center justify-center gap-2 rounded-xl bg-lime-400 px-7 py-3.5 sm:w-auto lg:mt-9 font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.45)] transition-all duration-300 ease-out hover:-translate-y-1 hover:bg-lime-300 hover:shadow-[0_0_30px_rgba(132,204,22,0.7)]"
                >
                  {activeRole.ctaLabel}
                  <ArrowRight className="h-5 w-5 transition-transform duration-300 ease-out group-hover:translate-x-1" />
                </Link>
              </div>

              {/* Visual */}
              <div className="group relative">
                <div
                  aria-hidden="true"
                  className="absolute -inset-4 rounded-[2rem] bg-gradient-to-tr from-emerald-500/20 to-lime-400/20 blur-2xl"
                />
                <div className="relative overflow-hidden rounded-3xl shadow-2xl shadow-emerald-900/20 ring-1 ring-emerald-900/10">
                  <img
                    src={activeRole.image}
                    alt={activeRole.title}
                    className="aspect-4/3 w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-emerald-950/60 via-transparent to-transparent" />

                  {/* Floating stat chip */}
                  <div className="absolute bottom-5 left-5 rounded-2xl border border-white/20 bg-white/15 px-5 py-3 shadow-lg backdrop-blur-xl transition-all duration-300 ease-out group-hover:-translate-y-1 group-hover:border-lime-300/40">
                    <p className="bg-gradient-to-r from-lime-200 to-emerald-200 bg-clip-text text-2xl font-bold text-transparent">
                      {activeRole.stat.value}
                    </p>
                    <p className="text-xs font-medium uppercase tracking-wide text-white/80">
                      {activeRole.stat.label}
                    </p>
                  </div>
                </div>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </section>
  );
}

export default RoleEcosystem;
