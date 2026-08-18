import { Star } from "lucide-react";
import TiltCard from "../ui/TiltCard";
import { RevealGroup, RevealItem } from "../motion/Reveal";
import tractorPloughingImage from "../../assets/images/services Image/Tractor Ploughing.jpeg";
import rotavatorWorkImage from "../../assets/images/services Image/Rotavator Work.jpeg";
import seederSowingImage from "../../assets/images/services Image/Seeder & Sowing.jpeg";
import harvesterServiceImage from "../../assets/images/services Image/Harvester Service.jpeg";
import droneSprayingImage from "../../assets/images/services Image/Drone Spraying.jpeg";
import cropSprayingTeamsImage from "../../assets/images/services Image/Crop Spraying Teams.jpeg";
import irrigationSetupImage from "../../assets/images/services Image/Irrigation Setup.jpeg";
import soilTestingImage from "../../assets/images/services Image/Soil Testing.jpeg";
import farmLaborImage from "../../assets/images/services Image/Farm Labor.jpeg";
import customFieldJobsImage from "../../assets/images/services Image/Custom Field Jobs.jpeg";

/**
 * Bento layout math (4-column grid at lg):
 *   row 1  →  Tractor Ploughing (2x2) | Harvester Service (2x1)
 *   row 2  →  ...................... | Drone Spraying | Seeder & Sowing
 *   row 3  →  Irrigation | Crop Spraying | Soil Testing (2x1)
 *   row 4  →  Farm Labor (2x1) | Rotavator Work | Custom Field Jobs
 * Every row sums to exactly 4 columns, so the grid tiles with no gaps.
 */
const servicesData = [
  {
    id: "tractor-ploughing",
    name: "Tractor Ploughing",
    description:
      "Deep soil preparation with modern tractor units for all crop types. Suitable for wheat, rice, and cotton fields.",
    price: "₹800/acre",
    rating: 4.8,
    image: tractorPloughingImage,
    badge: "Most Booked",
    span: "lg:col-span-2 lg:row-span-2",
    featured: true,
  },
  {
    id: "harvester-service",
    name: "Harvester Service",
    description:
      "Combine harvesters for wheat, rice, maize and other major crops. Minimum 3 acres.",
    price: "₹1,200/acre",
    rating: 4.9,
    image: harvesterServiceImage,
    badge: "High Demand",
    span: "lg:col-span-2",
  },
  {
    id: "drone-spraying",
    name: "Drone Spraying",
    description:
      "GPS-guided drone spraying for pesticides, fertilizers, and crop mapping at 10x the speed of manual methods.",
    price: "₹450/acre",
    rating: 4.8,
    image: droneSprayingImage,
    badge: "New",
    span: "",
  },
  {
    id: "seeder-sowing",
    name: "Seeder & Sowing",
    description:
      "Precision seed placement machines for consistent crop stands and optimal germination rates.",
    price: "₹500/acre",
    rating: 4.7,
    image: seederSowingImage,
    span: "",
  },
  {
    id: "irrigation-setup",
    name: "Irrigation Setup",
    description:
      "Drip, sprinkler, and flood irrigation installation and operation for all field types.",
    price: "₹250/hr",
    rating: 4.6,
    image: irrigationSetupImage,
    span: "",
  },
  {
    id: "crop-spraying-teams",
    name: "Crop Spraying Teams",
    description:
      "Skilled spray operators with backpack and boom sprayers for all crops and field sizes.",
    price: "₹300/acre",
    rating: 4.5,
    image: cropSprayingTeamsImage,
    span: "",
  },
  {
    id: "soil-testing",
    name: "Soil Testing",
    description:
      "On-site and lab soil health analysis with full NPK nutrient reports and crop-specific recommendations.",
    price: "₹199/visit",
    rating: 4.7,
    image: soilTestingImage,
    span: "lg:col-span-2",
  },
  {
    id: "farm-labor",
    name: "Farm Labor",
    description:
      "Verified farm hands and machine operators available by the day for all types of field work.",
    price: "₹450/day",
    rating: 4.5,
    image: farmLaborImage,
    span: "lg:col-span-2",
  },
  {
    id: "rotavator-work",
    name: "Rotavator Work",
    description:
      "Fine tillage and seedbed preparation for optimal germination and weed control.",
    price: "₹600/acre",
    rating: 4.6,
    image: rotavatorWorkImage,
    span: "",
  },
  {
    id: "custom-field-jobs",
    name: "Custom Field Jobs",
    description:
      "Bespoke per-field services tailored to your exact crop, terrain, and requirements.",
    price: "Custom",
    rating: 4.6,
    image: customFieldJobsImage,
    span: "",
  },
];

function ServiceCard({ service }) {
  const { name, description, price, rating, image, badge, featured } = service;

  return (
    <article
      className="group relative isolate h-full min-h-[44px] overflow-hidden rounded-2xl bg-emerald-950 shadow-xl shadow-emerald-900/10 ring-1 ring-slate-900/5 transition-shadow duration-300 ease-out hover:shadow-2xl hover:shadow-emerald-900/25 sm:rounded-3xl sm:shadow-2xl"
    >
      <img
        src={image}
        alt={name}
        loading="lazy"
        className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-110"
      />

      {/* Legibility scrim */}
      <div className="absolute inset-0 bg-gradient-to-t from-emerald-950 via-emerald-950/55 to-transparent transition-opacity duration-300 ease-out group-hover:from-emerald-950 group-hover:via-emerald-950/65" />

      {badge && (
        <span className="absolute left-5 top-5 rounded-full border border-white/20 bg-white/15 px-3 py-1 text-xs font-semibold text-white backdrop-blur-md transition-all duration-300 ease-out group-hover:border-lime-300/50 group-hover:bg-lime-400/25">
          {badge}
        </span>
      )}

      <div className="relative flex h-full flex-col justify-end p-5 sm:p-6">
        <h3
          className={`font-semibold text-white ${
            featured
              ? "font-display text-2xl sm:text-3xl"
              : "text-base sm:text-lg"
          }`}
        >
          {name}
        </h3>
        <p
          className={`mt-2 text-xs leading-relaxed text-white/70 sm:text-sm ${
            featured ? "max-w-md" : "line-clamp-2"
          }`}
        >
          {description}
        </p>

        <div className="mt-4 flex items-center justify-between border-t border-white/15 pt-3 sm:mt-5 sm:pt-4">
          <span className="bg-gradient-to-r from-lime-300 to-emerald-300 bg-clip-text text-base font-bold text-transparent sm:text-lg">
            {price}
          </span>
          <span className="flex items-center gap-1.5 rounded-full border border-white/15 bg-white/10 px-2.5 py-1 text-xs font-medium text-white backdrop-blur-md sm:px-3 sm:text-sm">
            <Star className="h-4 w-4 text-amber-300" fill="currentColor" />
            {rating}
          </span>
        </div>
      </div>
    </article>
  );
}

function ServicesGrid() {
  return (
    <section id="services" className="scroll-mt-24 bg-slate-50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:py-24">
        <div className="mx-auto max-w-2xl text-center">
          <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-widest text-emerald-700 sm:px-4 sm:text-xs">
            Agriculture Service Scope
          </span>
          <h2 className="mt-4 font-display text-3xl font-bold tracking-tight text-emerald-950 sm:mt-5 sm:text-4xl lg:text-5xl">
            10 Services. One Platform.
          </h2>
          <p className="mt-3 text-base leading-relaxed text-slate-600 sm:mt-4 sm:text-lg">
            Book by the acre, hour, or job. Every service priced transparently
            before you confirm.
          </p>
        </div>

        <RevealGroup
          className="mt-10 grid auto-rows-[240px] grid-cols-1 gap-4 [perspective:1200px] sm:auto-rows-[220px] sm:grid-cols-2 sm:gap-5 lg:mt-14 lg:auto-rows-[240px] lg:grid-cols-4"
          stagger={0.07}
          amount={0.1}
        >
          {servicesData.map((service) => (
            <RevealItem key={service.id} className={service.span}>
              <TiltCard className="h-full transition-transform duration-300 ease-out hover:-translate-y-2">
                <ServiceCard service={service} />
              </TiltCard>
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  );
}

export default ServicesGrid;
