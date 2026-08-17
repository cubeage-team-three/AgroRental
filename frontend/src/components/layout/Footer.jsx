import { Link } from "react-router-dom";
import { Sprout, Phone, MessageCircle, ArrowUpRight } from "lucide-react";

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
  return (
    <footer className="relative isolate overflow-hidden bg-emerald-950 text-white">
      {/* Gradient hairline + ambient glow */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-lime-400/50 to-transparent" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_0%,rgba(16,185,129,0.14),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-6 py-16">
        <div className="grid grid-cols-1 gap-12 sm:grid-cols-2 lg:grid-cols-4">
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
              <ul className="mt-5 space-y-3">
                {links.map((link) => (
                  <li key={link.label}>
                    <Link
                      to={link.to}
                      className="group inline-flex items-center gap-1.5 text-sm text-white/60 transition-all duration-300 ease-out hover:translate-x-1 hover:text-lime-300"
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
              className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-lime-300/40 hover:text-white"
            >
              <Phone className="h-4 w-4 text-lime-300" />
              1800-XXX-XXXX (Toll Free)
            </a>
            <a
              href="https://wa.me/919876543210"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-lime-300/40 hover:text-white"
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
