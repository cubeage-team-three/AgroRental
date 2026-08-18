import { motion, useReducedMotion } from "framer-motion";

/**
 * Scroll-triggered reveal primitives.
 *
 * <Reveal>            — single element, fades/slides in once on scroll.
 * <RevealGroup>       — parent that staggers its <RevealItem> children.
 * <RevealItem>        — child of RevealGroup; inherits the stagger timing.
 * <RevealText>        — splits a string into words and staggers them in.
 *
 * All of them collapse to a plain fade (or nothing) when the visitor has
 * "reduce motion" enabled at the OS level — an accessibility requirement,
 * not an optional nicety.
 */

const EASE = [0.22, 1, 0.36, 1]; // expo-out: fast start, soft landing

const offsets = {
  up: { y: 28, x: 0 },
  down: { y: -28, x: 0 },
  left: { x: 36, y: 0 },
  right: { x: -36, y: 0 },
  none: { x: 0, y: 0 },
};

export function Reveal({
  children,
  direction = "up",
  delay = 0,
  duration = 0.7,
  className = "",
  once = true,
  amount = 0.25,
  as = "div",
}) {
  const reduce = useReducedMotion();
  const offset = offsets[direction] ?? offsets.up;
  const MotionTag = motion[as] ?? motion.div;

  return (
    <MotionTag
      className={className}
      initial={reduce ? { opacity: 0 } : { opacity: 0, ...offset }}
      whileInView={{ opacity: 1, x: 0, y: 0 }}
      viewport={{ once, amount }}
      transition={{ duration: reduce ? 0.3 : duration, delay, ease: EASE }}
    >
      {children}
    </MotionTag>
  );
}

export function RevealGroup({
  children,
  className = "",
  stagger = 0.09,
  delayChildren = 0.05,
  once = true,
  amount = 0.2,
  as = "div",
}) {
  const reduce = useReducedMotion();
  const MotionTag = motion[as] ?? motion.div;

  return (
    <MotionTag
      className={className}
      initial="hidden"
      whileInView="visible"
      viewport={{ once, amount }}
      variants={{
        hidden: {},
        visible: {
          transition: {
            staggerChildren: reduce ? 0 : stagger,
            delayChildren: reduce ? 0 : delayChildren,
          },
        },
      }}
    >
      {children}
    </MotionTag>
  );
}

export function RevealItem({
  children,
  className = "",
  direction = "up",
  duration = 0.65,
  as = "div",
}) {
  const reduce = useReducedMotion();
  const offset = offsets[direction] ?? offsets.up;
  const MotionTag = motion[as] ?? motion.div;

  return (
    <MotionTag
      className={className}
      variants={{
        hidden: reduce ? { opacity: 0 } : { opacity: 0, ...offset },
        visible: {
          opacity: 1,
          x: 0,
          y: 0,
          transition: { duration: reduce ? 0.3 : duration, ease: EASE },
        },
      }}
    >
      {children}
    </MotionTag>
  );
}

/**
 * Word-by-word headline animation. Renders real text (each word is a plain
 * <span>), so it stays selectable and screen-reader friendly.
 */
export function RevealText({
  text,
  className = "",
  wordClassName = "",
  stagger = 0.06,
  delay = 0,
  once = true,
}) {
  const reduce = useReducedMotion();
  const words = String(text).split(" ");

  return (
    <motion.span
      className={className}
      initial="hidden"
      whileInView="visible"
      viewport={{ once, amount: 0.4 }}
      variants={{
        hidden: {},
        visible: {
          transition: {
            staggerChildren: reduce ? 0 : stagger,
            delayChildren: delay,
          },
        },
      }}
    >
      {words.map((word, index) => (
        <span
          key={`${word}-${index}`}
          className="inline-block overflow-hidden align-bottom"
        >
          <motion.span
            className={`inline-block ${wordClassName}`}
            variants={{
              hidden: reduce
                ? { opacity: 0 }
                : { opacity: 0, y: "0.9em", rotate: 2 },
              visible: {
                opacity: 1,
                y: 0,
                rotate: 0,
                transition: { duration: reduce ? 0.3 : 0.75, ease: EASE },
              },
            }}
          >
            {word}
          </motion.span>
          {index < words.length - 1 && <span>&nbsp;</span>}
        </span>
      ))}
    </motion.span>
  );
}
