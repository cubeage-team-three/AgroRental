import {
  Navigation,
  ShieldCheck,
  Globe2,
  IndianRupee,
  BarChart3,
  Clock,
} from "lucide-react";

const capabilities = [
  {
    icon: Navigation,
    title: "Live GPS Tracking",
    description:
      "Mandatory GPS for operators. Real-time location during jobs with ETA display and geo-fenced zones.",
  },
  {
    icon: ShieldCheck,
    title: "Verified Operators",
    description:
      "Document and license verification, admin approval before any operator goes live.",
  },
  {
    icon: Globe2,
    title: "9 Regional Languages",
    description:
      "Full support in Hindi, Tamil, Telugu, Kannada, Marathi, Bengali, Gujarati, Punjabi, Malayalam.",
  },
  {
    icon: IndianRupee,
    title: "Secure Payments",
    description:
      "UPI, card, and wallet support. Advance token + balance after completion. Auto invoices.",
  },
  {
    icon: BarChart3,
    title: "Admin Analytics",
    description:
      "Live maps, region heatmaps, seasonal demand trends, crop-wise analytics, utilization reports.",
  },
  {
    icon: Clock,
    title: "Seasonal Demand",
    description:
      "Pricing rules and capacity management adjust for sowing and harvest seasons automatically.",
  },
];

function PlatformCapabilities() {
  return (
    <section className="relative isolate overflow-hidden bg-emerald-950">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_0%,rgba(132,204,22,0.14),transparent_55%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_100%,rgba(16,185,129,0.18),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-6 py-24">
        <div className="mx-auto max-w-2xl text-center">
          <span className="inline-flex items-center rounded-full border border-lime-300/30 bg-lime-400/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-lime-300 backdrop-blur-md">
            Platform Capabilities
          </span>
          <h2 className="mt-5 font-display text-4xl font-bold tracking-tight text-white sm:text-5xl">
            Built for Real Agricultural Workflows
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-white/60">
            Not a generic marketplace — every capability is shaped by how
            fieldwork actually happens.
          </p>
        </div>

        <div className="mt-14 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {capabilities.map(({ icon: Icon, title, description }) => (
            <div
              key={title}
              className="group relative isolate overflow-hidden rounded-3xl border border-white/10 bg-white/5 p-7 shadow-2xl shadow-emerald-950/40 backdrop-blur-xl transition-all duration-300 ease-out hover:-translate-y-2 hover:border-lime-300/40 hover:bg-white/10 hover:shadow-lime-400/10"
            >
              <span
                aria-hidden="true"
                className="pointer-events-none absolute -right-12 -top-12 h-36 w-36 rounded-full bg-gradient-to-br from-lime-300/25 to-emerald-400/25 opacity-0 blur-2xl transition-opacity duration-300 ease-out group-hover:opacity-100"
              />

              <span className="relative flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/30 to-lime-400/20 shadow-lg transition-all duration-300 ease-out group-hover:scale-110 group-hover:border-lime-300/40">
                <Icon className="h-6 w-6 text-lime-300" />
              </span>

              <h3 className="relative mt-6 text-lg font-semibold text-white">
                {title}
              </h3>
              <p className="relative mt-2 text-sm leading-relaxed text-white/60 transition-colors duration-300 ease-out group-hover:text-white/75">
                {description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default PlatformCapabilities;
