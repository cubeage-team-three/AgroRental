import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Sprout,
  Phone,
  MessageCircle,
  ArrowUpRight,
  Send,
  Check,
} from "lucide-react";
import { Reveal } from "../motion/Reveal";

const footerLinks = {
  Services: [
    { label: "Tractor Ploughing", to: "/" },
    { label: "Harvester", to: "/" },
    { label: "Drone Spraying", to: "/" },
    { label: "Irrigation Setup", to: "/" },
    { label: "Soil Testing", to: "/" },
  ],
  Platform: [
    { label: "Farmer Portal", to: "/register/farmer" },
    { label: "Operator Portal", to: "/register/operator" },
    { label: "Equipment Owner", to: "/register/partner" },
    { label: "Admin Panel", to: "/login" },
    { label: "WhatsApp Booking", to: "/" },
  ],
  Company: [
    { label: "About Us", to: "/" },
    { label: "Contact", to: "/" },
    { label: "Careers", to: "/" },
    { label: "Partner with Us", to: "/register/partner" },
    { label: "Privacy Policy", to: "/" },
  ],
};

function Footer() {
  const [email, setEmail] = useState("");
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = (event) => {
    event.preventDefault();
    if (!email.trim()) return;
    // No backend endpoint yet — this only confirms locally.
    setSubscribed(true);
    setEmail("");
  };

  return (
    <footer className="relative isolate overflow-hidden bg-emerald-950 text-white">
      {/* Gradient hairline + ambient glow */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-lime-400/50 to-transparent" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_0%,rgba(16,185,129,0.14),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-14 lg:py-16">
        {/* Newsletter */}
        <Reveal className="mb-14 overflow-hidden rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl shadow-emerald-950/40 backdrop-blur-xl sm:p-8 lg:mb-16">
          <div className="relative grid grid-cols-1 items-center gap-6 lg:grid-cols-2 lg:gap-10">
            <span
              aria-hidden="true"
              className="pointer-events-none absolute -left-20 -top-24 h-56 w-56 rounded-full bg-lime-400/20 blur-3xl"
            />
            <div className="relative">
              <h3 className="font-display text-2xl font-bold tracking-tight text-white sm:text-3xl">
                Seasonal pricing, straight to your inbox
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-white/55 sm:text-base">
                Sowing and harvest rate updates, new machines near you, and
                nothing else.
              </p>
            </div>

            <form onSubmit={handleSubscribe} className="relative">
              <div className="group relative">
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute -inset-0.5 rounded-2xl bg-gradient-to-r from-lime-400/0 via-lime-400/40 to-emerald-400/0 opacity-0 blur-md transition-opacity duration-500 ease-out group-focus-within:opacity-100"
                />
                <div className="relative flex flex-col gap-3 rounded-2xl border border-white/15 bg-emerald-950/60 p-2 backdrop-blur-md transition-colors duration-300 ease-out focus-within:border-lime-300/60 sm:flex-row sm:items-center">
                  <label htmlFor="footer-email" className="sr-only">
                    Email address
                  </label>
                  <input
                    id="footer-email"
                    type="email"
                    required
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="you@farm.in"
                    className="min-h-[44px] w-full flex-1 bg-transparent px-4 text-sm text-white placeholder:text-white/35 focus:outline-none"
                  />
                  <button
                    type="submit"
                    className="group/btn inline-flex min-h-[48px] shrink-0 items-center justify-center gap-2 rounded-xl bg-lime-400 px-6 text-sm font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.45)] transition-all duration-300 ease-out hover:bg-lime-300 hover:shadow-[0_0_28px_rgba(132,204,22,0.7)]"
                  >
                    {subscribed ? (
                      <>
                        <Check className="h-4 w-4" strokeWidth={3} />
                        Subscribed
                      </>
                    ) : (
                      <>
                        Subscribe
                        <Send className="h-4 w-4 transition-transform duration-300 ease-out group-hover/btn:translate-x-0.5" />
                      </>
                    )}
                  </button>
                </div>
              </div>
              <p
                aria-live="polite"
                className={`mt-2 pl-1 text-xs transition-opacity duration-300 ${
                  subscribed ? "text-lime-300 opacity-100" : "opacity-0"
                }`}
              >
                Thanks — we&apos;ll be in touch before the next season.
              </p>
            </form>
          </div>
        </Reveal>

        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 sm:gap-12 lg:grid-cols-4">
          <div>
            <div className="flex items-center gap-2.5">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-500 to-lime-400 shadow-lg shadow-lime-400/20">
                <Sprout className="h-5 w-5 text-emerald-950" />
              </span>
              <span className="text-xl font-bold tracking-tight">
                Agro
                <span className="bg-gradient-to-r from-lime-300 to-emerald-300 bg-clip-text text-transparent">
                  Rent
                </span>
              </span>
            </div>
            <p className="mt-5 max-w-xs text-sm leading-relaxed text-white/50">
              India&apos;s agriculture service marketplace — connecting farmers
              with equipment and verified operators.
            </p>
          </div>

          {Object.entries(footerLinks).map(([heading, links]) => (
            <div key={heading}>
              <h3 className="text-xs font-semibold uppercase tracking-widest text-lime-300/80">
                {heading}
              </h3>
              <ul className="mt-4 space-y-0.5 lg:mt-5 lg:space-y-2">
                {links.map((link) => (
                  <li key={link.label}>
                    <Link
                      to={link.to}
                      className="group inline-flex min-h-[44px] items-center gap-1.5 text-sm text-white/60 transition-all duration-300 ease-out hover:translate-x-1 hover:text-lime-300 lg:min-h-0 lg:py-1"
                    >
                      {link.label}
                      <ArrowUpRight className="h-3 w-3 opacity-0 transition-opacity duration-300 ease-out group-hover:opacity-100" />
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col items-center justify-between gap-5 border-t border-white/10 pt-7 text-sm text-white/45 sm:flex-row">
          <p>&copy; {new Date().getFullYear()} AgroRent. All rights reserved.</p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <a
              href="tel:1800XXXXXXX"
              className="inline-flex min-h-[44px] items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-lime-300/40 hover:text-white"
            >
              <Phone className="h-4 w-4 text-lime-300" />
              1800-XXX-XXXX (Toll Free)
            </a>
            <a
              href="https://wa.me/919876543210"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-[44px] items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-lime-300/40 hover:text-white"
            >
              <MessageCircle className="h-4 w-4 text-lime-300" />
              +91 98765 43210
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
