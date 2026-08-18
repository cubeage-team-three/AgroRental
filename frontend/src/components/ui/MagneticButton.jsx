import { useRef } from "react";
import {
  motion,
  useMotionValue,
  useSpring,
  useReducedMotion,
} from "framer-motion";

/**
 * Magnetic wrapper — the child drifts toward the cursor while it's nearby,
 * then springs back on exit. Pointer-type aware: touch devices and
 * reduce-motion users get a plain static wrapper, since "magnetism" with no
 * cursor is just jitter.
 *
 * Renders a <span> wrapper, so it can safely hold a <Link> or <button>.
 */
function MagneticButton({ children, className = "", strength = 0.35 }) {
  const ref = useRef(null);
  const reduce = useReducedMotion();

  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const springX = useSpring(x, { stiffness: 220, damping: 18, mass: 0.6 });
  const springY = useSpring(y, { stiffness: 220, damping: 18, mass: 0.6 });

  const handleMove = (event) => {
    if (reduce) return;
    // Ignore touch/pen — magnetism only makes sense with a real cursor.
    if (event.pointerType && event.pointerType !== "mouse") return;

    const el = ref.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const offsetX = event.clientX - (rect.left + rect.width / 2);
    const offsetY = event.clientY - (rect.top + rect.height / 2);
    x.set(offsetX * strength);
    y.set(offsetY * strength);
  };

  const reset = () => {
    x.set(0);
    y.set(0);
  };

  return (
    <motion.span
      ref={ref}
      onPointerMove={handleMove}
      onPointerLeave={reset}
      onPointerCancel={reset}
      style={{ x: springX, y: springY }}
      className={`relative inline-flex ${className}`}
    >
      {children}
    </motion.span>
  );
}

export default MagneticButton;
