import {
  Wheat,
  Clock,
  Tractor,
  ClipboardList,
  CreditCard,
  MapPin,
  ShieldCheck,
  Calculator,
  ReceiptText,
} from "lucide-react";

const pricingModels = [
  {
    icon: Wheat,
    title: "Per Acre",
    description: "Tractor ploughing, rotavator, seeder",
  },
  {
    icon: Clock,
    title: "Per Hour",
    description: "Irrigation setup, custom operators",
  },
  {
    icon: Tractor,
    title: "Per Machine",
    description: "Harvester booking, dedicated fleet",
  },
  {
    icon: ClipboardList,
    title: "Per Job",
    description: "Soil testing visit, custom field jobs",
  },
  {
    icon: CreditCard,
    title: "Minimum Charge",
    description: "Drone spraying below 2 acres",
  },
  {
    icon: MapPin,
    title: "Travel Surcharge",
    description: "Service beyond defined radius",
  },
];

const trustPoints = [
  { icon: Calculator, label: "Live cost estimate before you confirm" },
  { icon: ShieldCheck, label: "No hidden charges" },
  { icon: ReceiptText, label: "GST-ready auto invoices" },
];

function PricingCards() {
  return (
    <section
      id="pricing"
      className="relative isolate scroll-mt-24 overflow-hidden bg-slate-50"
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(132,204,22,0.10),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-6 py-24">
        <div className="mx-auto max-w-2xl text-center">
          <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-emerald-700">
            Flexible Pricing
          </span>
          <h2 className="mt-5 font-display text-4xl font-bold tracking-tight text-emerald-950 sm:text-5xl">
            Pricing That Fits Every Job
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-slate-600">
            Six ways to price a job — you always see the full breakdown before
            confirming.
          </p>
        </div>

        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {pricingModels.map(({ icon: Icon, title, description }) => (
            <div
              key={title}
              className="group relative isolate overflow-hidden rounded-3xl border border-slate-200/70 bg-white p-7 shadow-xl shadow-emerald-900/5 transition-all duration-300 ease-out hover:-translate-y-2 hover:border-emerald-300 hover:shadow-2xl hover:shadow-emerald-900/15"
            >
              {/* Hover wash */}
              <span
                aria-hidden="true"
                className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-gradient-to-br from-emerald-400/20 to-lime-300/20 opacity-0 blur-2xl transition-opacity duration-300 ease-out group-hover:opacity-100"
              />

              <span className="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-600 to-emerald-500 shadow-lg shadow-emerald-600/25 transition-all duration-300 ease-out group-hover:scale-110 group-hover:shadow-emerald-600/40">
                <Icon className="h-6 w-6 text-white" />
              </span>

              <h3 className="relative mt-6 text-lg font-semibold text-emerald-950">
                {title}
              </h3>
              <p className="relative mt-1.5 text-sm leading-relaxed text-slate-500">
                {description}
              </p>
            </div>
          ))}
        </div>

        {/* Trust strip */}
        <div className="mx-auto mt-12 flex max-w-5xl flex-wrap items-center justify-center gap-x-10 gap-y-4 rounded-2xl border border-slate-200/70 bg-white/70 px-8 py-5 shadow-sm backdrop-blur-md">
          {trustPoints.map(({ icon: Icon, label }) => (
            <span
              key={label}
              className="flex items-center gap-2 text-sm font-medium text-slate-600 transition-colors duration-300 ease-out hover:text-emerald-800"
            >
              <Icon className="h-4 w-4 text-emerald-600" />
              {label}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}

export default PricingCards;
