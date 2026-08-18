import { useEffect, useRef, useState } from "react";
import { useInView, animate } from "framer-motion";
import { Users, ShieldCheck, Tractor, Globe2, MapPin, Star } from "lucide-react";

const stats = [
  { value: "50,000+", label: "Farmers Served", Icon: Users },
  { value: "12,000+", label: "Verified Operators", Icon: ShieldCheck },
  { value: "3,200+", label: "Machines Listed", Icon: Tractor },
  { value: "9", label: "Regional Languages", Icon: Globe2 },
  { value: "28", label: "States Covered", Icon: MapPin },
  { value: "4.8★", label: "Average Rating", Icon: Star },
];

function AnimatedValue({ value }) {
  const match = value.match(/^([\d,]+\.?\d*)(.*)$/);
  const numericPart = match ? match[1].replace(/,/g, "") : "0";
  const suffix = match ? match[2] : "";
  const isDecimal = numericPart.includes(".");
  const target = parseFloat(numericPart);

  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: "-80px" });
  const [display, setDisplay] = useState(isDecimal ? "0.0" : "0");

  useEffect(() => {
    if (!isInView) return undefined;
    const controls = animate(0, target, {
      duration: 1.8,
      ease: "easeOut",
      onUpdate: (latest) => {
        setDisplay(
          isDecimal
            ? latest.toFixed(1)
            : Math.round(latest).toLocaleString("en-US"),
        );
      },
    });
    return () => controls.stop();
  }, [isInView, target, isDecimal]);

  return (
    <span ref={ref}>
      {display}
      {suffix}
    </span>
  );
}

function StatsBanner() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-forest-900 via-green-800 to-forest-950">
      <div
        className="absolute inset-0 opacity-[0.07] mix-blend-overlay"
        style={{
          backgroundImage:
            "repeating-linear-gradient(135deg, rgba(255,255,255,0.7) 0px, rgba(255,255,255,0.7) 1px, transparent 1px, transparent 14px)",
        }}
        aria-hidden="true"
      />

      <div className="relative mx-auto grid max-w-7xl grid-cols-2 gap-8 px-6 py-12 text-center sm:grid-cols-3 lg:grid-cols-6">
        {stats.map(({ value, label, Icon }) => (
          <div key={label} className="relative flex flex-col items-center py-2">
            <Icon
              className="pointer-events-none absolute left-1/2 top-1/2 h-16 w-16 -translate-x-1/2 -translate-y-1/2 text-green-300/10 sm:h-20 sm:w-20"
              aria-hidden="true"
            />
            <p className="relative text-2xl font-bold text-white sm:text-3xl">
              <AnimatedValue value={value} />
            </p>
            <p className="relative mt-1 text-sm text-green-100">{label}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

export default StatsBanner;
