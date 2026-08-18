import { Link } from "react-router-dom";
import { ArrowRight, ShieldCheck, Navigation, Globe2 } from "lucide-react";
import ctaImage from "../../assets/images/hero-golden-hour.jpg";

const assurances = [
  { icon: ShieldCheck, label: "Verified operators" },
  { icon: Navigation, label: "Live GPS tracking" },
  { icon: Globe2, label: "9 regional languages" },
];

function CallToAction() {
  return (
    <section className="relative isolate overflow-hidden bg-emerald-950">
      <img
        src={ctaImage}
        alt=""
        aria-hidden="true"
        className="absolute inset-0 h-full w-full object-cover"
      />

      {/* Cinematic grading */}
      <div className="absolute inset-0 bg-gradient-to-b from-emerald-950/85 via-emerald-950/90 to-emerald-950/95" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(132,204,22,0.18),transparent_60%)]" />

      <div className="relative mx-auto max-w-4xl px-6 py-28">
        <div className="rounded-[2rem] border border-white/15 bg-white/5 p-10 text-center shadow-2xl shadow-emerald-950/50 backdrop-blur-xl sm:p-14">
          <h2 className="font-display text-4xl font-bold leading-tight tracking-tight text-white sm:text-5xl">
            Ready to Transform Your{" "}
            <span className="bg-gradient-to-r from-lime-300 to-emerald-300 bg-clip-text text-transparent">
              Farm Operations?
            </span>
          </h2>
          <p className="mt-5 text-lg leading-relaxed text-white/70">
            Join 50,000+ farmers already booking equipment on AgroRent.
          </p>

          <div className="mt-10 flex flex-wrap justify-center gap-4">
            <div className="relative">
              <span
                aria-hidden="true"
                className="absolute -inset-1 animate-pulse rounded-xl bg-lime-400/40 blur-lg"
              />
              <Link
                to="/register"
                className="group relative inline-flex items-center gap-2 rounded-xl bg-lime-400 px-8 py-4 font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.5)] transition-all duration-300 ease-out hover:-translate-y-1 hover:bg-lime-300 hover:shadow-[0_0_30px_rgba(132,204,22,0.75)]"
              >
                Create Free Account
                <ArrowRight className="h-5 w-5 transition-transform duration-300 ease-out group-hover:translate-x-1" />
              </Link>
            </div>

            <Link
              to="/login"
              className="inline-flex items-center gap-2 rounded-xl border border-white/25 bg-white/10 px-8 py-4 font-semibold text-white backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-1 hover:border-white/40 hover:bg-white/20"
            >
              Log In to AgroRent
            </Link>
          </div>

          <div className="mt-10 flex flex-wrap items-center justify-center gap-x-8 gap-y-3 border-t border-white/10 pt-8">
            {assurances.map(({ icon: Icon, label }) => (
              <span
                key={label}
                className="flex items-center gap-2 text-sm text-white/60 transition-colors duration-300 ease-out hover:text-white"
              >
                <Icon className="h-4 w-4 text-lime-300" />
                {label}
              </span>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

export default CallToAction;
