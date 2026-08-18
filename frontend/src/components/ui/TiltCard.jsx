import { useRef } from "react";
import {
  motion,
  useMotionValue,
  useSpring,
  useTransform,
  useReducedMotion,
} from "framer-motion";

/**
 * Subtle 3D tilt on pointer move. Kept deliberately restrained (max ~6deg) —
 * heavy tilt reads as a gimmick and hurts text legibility on image cards.
 *
 * Disabled for touch pointers and reduce-motion users.
 */
function TiltCard({ children, className = "", max = 6 }) {
  const ref = useRef(null);
  const reduce = useReducedMotion();

  const px = useMotionValue(0.5);
  const py = useMotionValue(0.5);

  const springCfg = { stiffness: 200, damping: 20, mass: 0.5 };
  const rotateX = useSpring(
    useTransform(py, [0, 1], [max, -max]),
    springCfg,
  );
  const rotateY = useSpring(
    useTransform(px, [0, 1], [-max, max]),
    springCfg,
  );

  const handleMove = (event) => {
    if (reduce) return;
    if (event.pointerType && event.pointerType !== "mouse") return;
    const el = ref.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    px.set((event.clientX - rect.left) / rect.width);
    py.set((event.clientY - rect.top) / rect.height);
  };

  const reset = () => {
    px.set(0.5);
    py.set(0.5);
  };

  return (
    <motion.div
      ref={ref}
      onPointerMove={handleMove}
      onPointerLeave={reset}
      onPointerCancel={reset}
      style={{
        rotateX: reduce ? 0 : rotateX,
        rotateY: reduce ? 0 : rotateY,
        transformStyle: "preserve-3d",
      }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

export default TiltCard;
