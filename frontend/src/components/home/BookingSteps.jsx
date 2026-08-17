import { MapPin, CheckCircle2, Navigation, ArrowRight } from "lucide-react";

const steps = [
  {
    number: "01",
    icon: MapPin,
    title: "Select Your Field",
    description:
      "Pin your farm on the map or enter your village/district. Add multiple plots with exact acre measurements.",
  },
  {
    number: "02",
    icon: CheckCircle2,
    title: "Choose Service & Slot",
    description:
      "Browse equipment and operators filtered by distance, price, and availability. See live cost estimate before confirming.",
  },
  {
    number: "03",
    icon: Navigation,
    title: "Track & Pay",
    description:
      "Watch your operator travel to your field on live GPS. Pay securely via UPI, card, or wallet. Get auto-generated invoice.",
  },
];

function BookingSteps() {
  return (
    <section
      id="how-it-works"
      className="relative isolate scroll-mt-24 overflow-hidden bg-emerald-950"
    >
      {/* Ambient depth */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_0%,rgba(16,185,129,0.18),transparent_55%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_85%_100%,rgba(132,204,22,0.14),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-6 py-24">
        <div className="mx-auto max-w-2xl text-center">
          <span className="inline-flex items-center rounded-full border border-lime-300/30 bg-lime-400/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-lime-300 backdrop-blur-md">
            Booking Flow
          </span>
          <h2 className="mt-5 font-display text-4xl font-bold tracking-tight text-white sm:text-5xl">
            Book in 3 Simple Steps
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-white/60">
            From pinning your plot to tracking the machine at your gate — the
            whole journey takes minutes.
          </p>
        </div>

        <div className="mt-16 grid grid-cols-1 gap-8 md:grid-cols-3 md:gap-6">
          {steps.map((step, index) => (
            <div key={step.number} className="relative">
              <div className="group h-full rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl shadow-emerald-950/50 backdrop-blur-xl transition-all duration-300 ease-out hover:-translate-y-2 hover:border-lime-300/40 hover:bg-white/10 hover:shadow-lime-400/10">
                <div className="flex items-start justify-between">
                  <span className="flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/30 to-lime-400/20 shadow-lg transition-all duration-300 ease-out group-hover:scale-110 group-hover:border-lime-300/40">
                    <step.icon className="h-6 w-6 text-lime-300" />
                  </span>
                  <span className="bg-gradient-to-br from-emerald-300 to-lime-300/60 bg-clip-text font-display text-6xl font-bold leading-none text-transparent transition-all duration-300 ease-out group-hover:from-emerald-200 group-hover:to-lime-200">
                    {step.number}
                  </span>
                </div>

                <h3 className="mt-8 text-xl font-semibold text-white">
                  {step.title}
                </h3>
                <p className="mt-3 text-sm leading-relaxed text-white/60 transition-colors duration-300 ease-out group-hover:text-white/75">
                  {step.description}
                </p>
              </div>

              {/* Connector between cards */}
              {index < steps.length - 1 && (
                <span className="absolute top-1/2 -right-3 z-10 hidden h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full border border-lime-300/40 bg-emerald-900 shadow-[0_0_15px_rgba(132,204,22,0.35)] md:flex">
                  <ArrowRight className="h-4 w-4 text-lime-300" />
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default BookingSteps;
