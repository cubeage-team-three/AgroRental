import { useEffect, useMemo, useRef, useState } from "react";
import { request } from "../../services/apiClient";
import {
  AnimatePresence,
  motion,
  useMotionValue,
  useMotionValueEvent,
  useSpring,
} from "framer-motion";
import {
  AlertCircle,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronLeft,
  Loader2,
  MapPin,
  Receipt,
  Ruler,
  ShieldCheck,
  Sparkles,
  Star,
  Tractor,
  Wallet,
  X,
} from "lucide-react";
import MagneticButton from "../ui/MagneticButton";
import { API_BASE_URL } from "../../utils/constants";
import { getFarmerId } from "../../services/authService";

const EASE = [0.22, 1, 0.36, 1];

const RATE_PER_ACRE_PER_DAY = 800;
const OPERATOR_FEE_PER_DAY = 350;
const PLATFORM_FEE_RATE = 0.02;
const ADVANCE_RATE = 0.25;

const DEFAULT_EQUIPMENT = {
  name: "John Deere 5050D Tractor",
  category: "Tractor · 50 HP",
  partner: "Patil AgroServices",
  rating: 4.8,
  image: null,
};

const INITIAL_FORM = {
  startDate: "",
  endDate: "",
  location: "",
  totalAcreage: "",
  requireOperator: true,
};

const STEPS = [
  { id: 1, label: "Schedule", icon: CalendarDays },
  { id: 2, label: "Requirement", icon: Ruler },
  { id: 3, label: "Estimate", icon: Receipt },
  { id: 4, label: "Confirm", icon: ShieldCheck },
];

function formatINR(value) {
  return Math.round(value || 0).toLocaleString("en-IN");
}

function calculateDays(startDate, endDate) {
  if (!startDate || !endDate) return 0;
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diff = Math.round((end - start) / 86_400_000) + 1;
  return Math.max(diff, 0);
}

async function submitBooking(payload) {
  return await request('/bookings', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

function validateStep(step, form) {
  const errors = {};
  if (step === 1) {
    if (!form.startDate) errors.startDate = "Start date is required";
    if (!form.endDate) errors.endDate = "End date is required";
    if (form.startDate && form.endDate && form.endDate < form.startDate) {
      errors.endDate = "End date must be after start date";
    }
    if (!form.location.trim()) errors.location = "Enter your farming location";
  }
  if (step === 2) {
    const acreage = Number(form.totalAcreage);
    if (!form.totalAcreage || acreage <= 0) {
      errors.totalAcreage = "Enter a valid acreage";
    }
  }
  return errors;
}

function FloatingField({ id, label, icon: Icon, error, className = "", ...props }) {
  const [focused, setFocused] = useState(false);
  const active = focused || (props.value !== undefined && props.value !== "" && props.value !== null);

  return (
    <div className={className}>
      <div
        className={`relative rounded-2xl border backdrop-blur-md transition-all duration-300 ease-out ${
          error
            ? "border-red-400/60 bg-red-500/5"
            : focused
              ? "border-lime-300/70 bg-white/10 shadow-[0_0_0_4px_rgba(163,230,53,0.15),0_0_28px_-6px_rgba(163,230,53,0.55)]"
              : "border-white/15 bg-white/5 hover:border-white/25"
        }`}
      >
        {Icon && (
          <Icon
            className={`pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 transition-colors duration-300 ${
              focused ? "text-lime-300" : "text-white/40"
            }`}
          />
        )}
        <input
          id={id}
          onFocus={(event) => {
            setFocused(true);
            props.onFocus?.(event);
          }}
          onBlur={(event) => {
            setFocused(false);
            props.onBlur?.(event);
          }}
          className={`peer min-h-[58px] w-full rounded-2xl bg-transparent px-4 pt-6 pb-2 text-[15px] text-white outline-none ${
            Icon ? "pl-11" : ""
          }`}
          {...props}
        />
        <label
          htmlFor={id}
          className={`pointer-events-none absolute transition-all duration-300 ease-out ${
            Icon ? "left-11" : "left-4"
          } ${
            active
              ? "top-[9px] text-[11px] font-medium tracking-wide text-lime-300/90"
              : "top-1/2 -translate-y-1/2 text-[15px] text-white/40"
          }`}
        >
          {label}
        </label>
      </div>
      {error && <p className="mt-1.5 pl-1 text-xs font-medium text-red-300">{error}</p>}
    </div>
  );
}

function ToggleSwitch({ checked, onChange }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className={`flex h-9 w-16 shrink-0 items-center rounded-full border p-1 transition-colors duration-300 ${
        checked
          ? "justify-end border-lime-300/60 bg-lime-400/25 shadow-[0_0_20px_-2px_rgba(163,230,53,0.6)]"
          : "justify-start border-white/15 bg-white/5"
      }`}
    >
      <motion.span
        layout
        transition={{ type: "spring", stiffness: 500, damping: 32 }}
        className={`flex h-7 w-7 items-center justify-center rounded-full shadow-md ${
          checked ? "bg-lime-400 text-emerald-950" : "bg-white/70 text-slate-500"
        }`}
      >
        {checked ? <Check className="h-3.5 w-3.5" /> : <X className="h-3.5 w-3.5" />}
      </motion.span>
    </button>
  );
}

function AnimatedCounter({ value, prefix = "₹", className = "" }) {
  const motionValue = useMotionValue(0);
  const spring = useSpring(motionValue, { stiffness: 90, damping: 20, mass: 1 });
  const [display, setDisplay] = useState(0);

  useEffect(() => {
    motionValue.set(value);
  }, [value, motionValue]);

  useMotionValueEvent(spring, "change", (latest) => setDisplay(Math.round(latest)));

  return (
    <span className={className}>
      {prefix}
      {display.toLocaleString("en-IN")}
    </span>
  );
}

function StepIndicator({ currentStep }) {
  const progress = ((currentStep - 1) / (STEPS.length - 1)) * 100;

  return (
    <div className="relative px-1 pt-1">
      <div className="absolute left-5 right-5 top-[19px] h-[2px] bg-white/10">
        <motion.div
          className="h-full bg-gradient-to-r from-emerald-400 to-lime-300"
          initial={false}
          animate={{ width: `${progress}%` }}
          transition={{ duration: 0.45, ease: EASE }}
        />
      </div>
      <div className="relative flex justify-between">
        {STEPS.map((s) => {
          const isDone = s.id < currentStep;
          const isActive = s.id === currentStep;
          const Icon = s.icon;
          return (
            <div key={s.id} className="flex flex-col items-center gap-1.5">
              <span
                className={`flex h-10 w-10 items-center justify-center rounded-full border-2 transition-all duration-300 ${
                  isActive
                    ? "border-lime-300 bg-emerald-900 text-lime-300 shadow-[0_0_18px_-2px_rgba(163,230,53,0.7)]"
                    : isDone
                      ? "border-emerald-400 bg-emerald-400 text-emerald-950"
                      : "border-white/15 bg-emerald-950 text-white/30"
                }`}
              >
                {isDone ? <Check className="h-4 w-4" /> : <Icon className="h-4 w-4" />}
              </span>
              <span
                className={`hidden text-[11px] font-medium sm:block ${
                  isActive ? "text-lime-300" : isDone ? "text-emerald-300/80" : "text-white/30"
                }`}
              >
                {s.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function StepSchedule({ form, errors, onChange }) {
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <FloatingField
          id="startDate"
          label="Start Date"
          icon={CalendarDays}
          type="date"
          min={today}
          value={form.startDate}
          error={errors.startDate}
          onChange={(e) => onChange({ startDate: e.target.value })}
        />
        <FloatingField
          id="endDate"
          label="End Date"
          icon={CalendarDays}
          type="date"
          min={form.startDate || today}
          value={form.endDate}
          error={errors.endDate}
          onChange={(e) => onChange({ endDate: e.target.value })}
        />
      </div>
      <FloatingField
        id="location"
        label="Farming Location / Village"
        icon={MapPin}
        type="text"
        placeholder=""
        value={form.location}
        error={errors.location}
        onChange={(e) => onChange({ location: e.target.value })}
      />
    </div>
  );
}

function StepRequirements({ form, errors, onChange }) {
  return (
    <div className="space-y-5">
      <FloatingField
        id="totalAcreage"
        label="Total Acreage"
        icon={Ruler}
        type="number"
        inputMode="decimal"
        min="0.1"
        step="0.1"
        value={form.totalAcreage}
        error={errors.totalAcreage}
        onChange={(e) => onChange({ totalAcreage: e.target.value })}
      />

      <div className="flex items-center justify-between rounded-2xl border border-white/15 bg-white/5 px-4 py-4 backdrop-blur-md">
        <div>
          <p className="text-sm font-semibold text-white">Require Operator?</p>
          <p className="mt-0.5 text-xs text-white/50">
            A trained operator will run the equipment for you.
          </p>
        </div>
        <ToggleSwitch
          checked={form.requireOperator}
          onChange={(value) => onChange({ requireOperator: value })}
        />
      </div>
    </div>
  );
}

function StepEstimate({ form, days, breakdown }) {
  return (
    <div className="space-y-4">
      <div className="rounded-2xl border border-lime-300/25 bg-gradient-to-b from-white/10 to-white/[0.03] p-5 shadow-[0_20px_60px_-25px_rgba(163,230,53,0.45)] backdrop-blur-md">
        <div className="flex items-center gap-2 text-lime-300">
          <Sparkles className="h-4 w-4" />
          <span className="text-[11px] font-semibold uppercase tracking-widest">
            Live Estimate
          </span>
        </div>

        <div className="mt-4 space-y-2.5 text-sm">
          <div className="flex items-center justify-between text-white/70">
            <span>
              Equipment rate ({form.totalAcreage || 0} acre × ₹{RATE_PER_ACRE_PER_DAY}/acre × {Math.max(days, 1)} day
              {Math.max(days, 1) > 1 ? "s" : ""})
            </span>
            <span className="font-medium text-white">₹{formatINR(breakdown.equipmentSubtotal)}</span>
          </div>
          {form.requireOperator && (
            <div className="flex items-center justify-between text-white/70">
              <span>
                Operator fee (₹{OPERATOR_FEE_PER_DAY}/day × {Math.max(days, 1)} day
                {Math.max(days, 1) > 1 ? "s" : ""})
              </span>
              <span className="font-medium text-white">₹{formatINR(breakdown.operatorTotal)}</span>
            </div>
          )}
          <div className="flex items-center justify-between text-white/70">
            <span>Platform fee ({(PLATFORM_FEE_RATE * 100).toFixed(0)}%)</span>
            <span className="font-medium text-white">₹{formatINR(breakdown.platformFee)}</span>
          </div>
        </div>

        <div className="my-4 h-px bg-gradient-to-r from-transparent via-white/15 to-transparent" />

        <div className="flex items-end justify-between">
          <span className="text-sm font-medium text-white/60">Total Cost</span>
          <AnimatedCounter
            value={breakdown.totalCost}
            className="bg-gradient-to-br from-lime-200 to-emerald-300 bg-clip-text font-display text-4xl font-bold text-transparent drop-shadow-[0_0_25px_rgba(163,230,53,0.45)]"
          />
        </div>

        <div className="mt-3 flex items-center justify-between rounded-xl border border-white/10 bg-black/20 px-3.5 py-2.5">
          <span className="flex items-center gap-1.5 text-xs text-white/60">
            <Wallet className="h-3.5 w-3.5" />
            Advance due now ({(ADVANCE_RATE * 100).toFixed(0)}%)
          </span>
          <span className="text-sm font-semibold text-lime-300">
            ₹{formatINR(breakdown.advanceAmount)}
          </span>
        </div>
      </div>
    </div>
  );
}

function StepConfirm({
  equipment,
  form,
  days,
  breakdown,
  status,
  isSubmitting,
  submitError,
  onConfirm,
  onBack,
  onClose,
}) {
  if (status === "success") {
    return (
      <div className="flex flex-col items-center py-6 text-center">
        <motion.span
          initial={{ scale: 0.4, opacity: 0, rotate: -20 }}
          animate={{ scale: 1, opacity: 1, rotate: 0 }}
          transition={{ type: "spring", stiffness: 260, damping: 18 }}
          className="flex h-16 w-16 items-center justify-center rounded-full bg-lime-400/15 text-lime-300 shadow-[0_0_35px_-4px_rgba(163,230,53,0.7)]"
        >
          <CheckCircle2 className="h-9 w-9" />
        </motion.span>
        <h3 className="mt-5 font-display text-2xl font-bold text-white">Booking Confirmed!</h3>
        <p className="mt-2 max-w-[280px] text-sm leading-relaxed text-white/60">
          Your advance payment of{" "}
          <span className="font-semibold text-lime-300">₹{formatINR(breakdown.advanceAmount)}</span>{" "}
          was received. The partner will confirm your slot shortly.
        </p>
        <div className="mt-4 rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-xs text-white/50">
          Booking Ref · AGR-{Math.floor(100000 + Math.random() * 900000)}
        </div>
        <button
          type="button"
          onClick={onClose}
          className="mt-6 flex min-h-[52px] w-full items-center justify-center rounded-2xl border border-white/15 bg-white/5 text-sm font-semibold text-white backdrop-blur-md transition-all duration-200 ease-out hover:border-white/25 hover:bg-white/10"
        >
          Done
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="space-y-2.5 rounded-2xl border border-white/15 bg-white/5 p-4 text-sm backdrop-blur-md">
        <div className="flex justify-between text-white/60">
          <span>Equipment</span>
          <span className="font-medium text-white">{equipment.name}</span>
        </div>
        <div className="flex justify-between text-white/60">
          <span>Dates</span>
          <span className="font-medium text-white">
            {form.startDate} → {form.endDate} ({Math.max(days, 1)} day{Math.max(days, 1) > 1 ? "s" : ""})
          </span>
        </div>
        <div className="flex justify-between text-white/60">
          <span>Location</span>
          <span className="font-medium text-white">{form.location}</span>
        </div>
        <div className="flex justify-between text-white/60">
          <span>Acreage</span>
          <span className="font-medium text-white">{form.totalAcreage} acres</span>
        </div>
        <div className="flex justify-between text-white/60">
          <span>Operator</span>
          <span className="font-medium text-white">{form.requireOperator ? "Included" : "Not required"}</span>
        </div>
        <div className="my-1 h-px bg-white/10" />
        <div className="flex justify-between text-base">
          <span className="font-semibold text-white">Total Cost</span>
          <span className="font-bold text-lime-300">₹{formatINR(breakdown.totalCost)}</span>
        </div>
      </div>

      {submitError && (
        <div className="flex items-start gap-2 rounded-xl border border-red-400/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{submitError}</span>
        </div>
      )}

      <MagneticButton className="w-full">
        <motion.button
          type="button"
          onClick={onConfirm}
          disabled={isSubmitting}
          animate={
            isSubmitting
              ? {}
              : {
                  boxShadow: [
                    "0 0 20px 0px rgba(163,230,53,0.45)",
                    "0 0 45px 8px rgba(163,230,53,0.75)",
                    "0 0 20px 0px rgba(163,230,53,0.45)",
                  ],
                }
          }
          transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
          className="flex min-h-[56px] w-full items-center justify-center gap-2 rounded-2xl bg-lime-400 px-6 text-[15px] font-semibold text-emerald-950 transition-transform duration-200 ease-out active:scale-[0.98] disabled:opacity-70"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="h-4.5 w-4.5 animate-spin" />
              Processing...
            </>
          ) : (
            <>Confirm Booking & Pay Advance ₹{formatINR(breakdown.advanceAmount)}</>
          )}
        </motion.button>
      </MagneticButton>

      {!isSubmitting && (
        <button
          type="button"
          onClick={onBack}
          className="w-full text-center text-xs font-medium text-white/40 transition-colors duration-200 hover:text-white/70"
        >
          ← Edit details
        </button>
      )}
    </div>
  );
}

function BookingModal({ isOpen, onClose, equipment = DEFAULT_EQUIPMENT, onConfirm }) {
  const [[step, direction], setStepState] = useState([1, 0]);
  const [form, setForm] = useState(INITIAL_FORM);
  const [errors, setErrors] = useState({});
  const [status, setStatus] = useState("form");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const closeTimerRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setStepState([1, 0]);
      setForm(INITIAL_FORM);
      setErrors({});
      setStatus("form");
      setIsSubmitting(false);
      setSubmitError(null);
      if (closeTimerRef.current) {
        clearTimeout(closeTimerRef.current);
        closeTimerRef.current = null;
      }
    }
  }, [isOpen]);

  useEffect(() => () => {
    if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
  }, []);

  useEffect(() => {
    if (!isOpen) return undefined;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKey = (event) => {
      if (event.key === "Escape" && !isSubmitting) onClose?.();
    };
    window.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = prevOverflow;
      window.removeEventListener("keydown", onKey);
    };
  }, [isOpen, isSubmitting, onClose]);

  const days = useMemo(() => calculateDays(form.startDate, form.endDate), [form.startDate, form.endDate]);

  const breakdown = useMemo(() => {
    const acreage = Number(form.totalAcreage) || 0;
    const activeDays = Math.max(days, 1);
    const equipmentSubtotal = acreage * RATE_PER_ACRE_PER_DAY * activeDays;
    const operatorTotal = form.requireOperator ? OPERATOR_FEE_PER_DAY * activeDays : 0;
    const platformFee = Math.round((equipmentSubtotal + operatorTotal) * PLATFORM_FEE_RATE);
    const totalCost = equipmentSubtotal + operatorTotal + platformFee;
    const advanceAmount = Math.round(totalCost * ADVANCE_RATE);
    return { equipmentSubtotal, operatorTotal, platformFee, totalCost, advanceAmount };
  }, [form.totalAcreage, form.requireOperator, days]);

  function updateForm(patch) {
    setForm((prev) => ({ ...prev, ...patch }));
  }

  function goTo(nextStep) {
    setStepState(([current]) => [nextStep, nextStep > current ? 1 : -1]);
  }

  function handleNext() {
    const stepErrors = validateStep(step, form);
    if (Object.keys(stepErrors).length) {
      setErrors(stepErrors);
      return;
    }
    setErrors({});
    goTo(Math.min(step + 1, STEPS.length));
  }

  function handleBack() {
    setErrors({});
    goTo(Math.max(step - 1, 1));
  }

  async function handleConfirm() {
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const booking = await submitBooking({
        farmerId: getFarmerId(),
        equipmentId: equipment.id || 1,
        startDate: form.startDate,
        endDate: form.endDate,
        totalAcreage: Number(form.totalAcreage),
      });
      setStatus("success");
      onConfirm?.(booking);
      closeTimerRef.current = setTimeout(() => {
        onClose?.();
      }, 2000);
    } catch (err) {
      const message =
        err.response?.data?.message ||
        (err.request ? "Could not reach the server. Please check your connection." : err.message) ||
        "Something went wrong. Please try again.";
      setSubmitError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleClose() {
    if (isSubmitting) return;
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    onClose?.();
  }

  const slideVariants = {
    enter: (dir) => ({ x: dir >= 0 ? 36 : -36, opacity: 0 }),
    center: { x: 0, opacity: 1 },
    exit: (dir) => ({ x: dir >= 0 ? -36 : 36, opacity: 0 }),
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-emerald-950/70 p-4 backdrop-blur-sm sm:p-6"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.25 }}
          onClick={handleClose}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            onClick={(event) => event.stopPropagation()}
            initial={{ opacity: 0, y: 28, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.96 }}
            transition={{ duration: 0.4, ease: EASE }}
            className="relative flex max-h-[92vh] w-full max-w-[480px] flex-col overflow-hidden rounded-3xl border border-white/15 bg-emerald-950/80 shadow-[0_30px_90px_-20px_rgba(0,0,0,0.6)] backdrop-blur-2xl"
          >
            <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_20%_0%,rgba(16,185,129,0.25),transparent_55%)]" />
            <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_85%_100%,rgba(132,204,22,0.16),transparent_55%)]" />

            <button
              type="button"
              onClick={handleClose}
              aria-label="Close booking dialog"
              className="absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/60 backdrop-blur-md transition-all duration-200 ease-out hover:border-white/25 hover:bg-white/10 hover:text-white"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="overflow-y-auto px-5 pb-6 pt-5 sm:px-6">
              <div className="flex items-center gap-3 pr-10">
                <span className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/30 to-lime-400/20 shadow-lg">
                  {equipment.image ? (
                    <img src={equipment.image} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <Tractor className="h-6 w-6 text-lime-300" />
                  )}
                </span>
                <div className="min-w-0">
                  <p className="truncate text-[10px] font-semibold uppercase tracking-widest text-lime-300/80">
                    Booking
                  </p>
                  <h2 className="truncate font-display text-base font-bold text-white sm:text-lg">
                    {equipment.name}
                  </h2>
                  <p className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-white/50">
                    {equipment.category} · {equipment.partner}
                    <span className="flex items-center gap-0.5 text-amber-300">
                      <Star className="h-3 w-3 fill-amber-300" />
                      {equipment.rating}
                    </span>
                  </p>
                </div>
              </div>

              {status !== "success" && (
                <div className="mt-6">
                  <StepIndicator currentStep={step} />
                </div>
              )}

              <div className="relative mt-6 min-h-[220px] overflow-hidden">
                <AnimatePresence mode="wait" custom={direction} initial={false}>
                  <motion.div
                    key={status === "success" ? "success" : step}
                    custom={direction}
                    variants={slideVariants}
                    initial="enter"
                    animate="center"
                    exit="exit"
                    transition={{ duration: 0.32, ease: EASE }}
                  >
                    {status !== "success" && step === 1 && (
                      <StepSchedule form={form} errors={errors} onChange={updateForm} />
                    )}
                    {status !== "success" && step === 2 && (
                      <StepRequirements form={form} errors={errors} onChange={updateForm} />
                    )}
                    {status !== "success" && step === 3 && (
                      <StepEstimate form={form} days={days} breakdown={breakdown} />
                    )}
                    {step === 4 && (
                      <StepConfirm
                        equipment={equipment}
                        form={form}
                        days={days}
                        breakdown={breakdown}
                        status={status}
                        isSubmitting={isSubmitting}
                        submitError={submitError}
                        onConfirm={handleConfirm}
                        onBack={handleBack}
                        onClose={handleClose}
                      />
                    )}
                  </motion.div>
                </AnimatePresence>
              </div>

              {status === "form" && step < 4 && (
                <div className="mt-6 flex items-center gap-3">
                  {step > 1 && (
                    <button
                      type="button"
                      onClick={handleBack}
                      className="flex min-h-[52px] items-center gap-1.5 rounded-2xl border border-white/15 bg-white/5 px-5 text-sm font-semibold text-white/80 backdrop-blur-md transition-all duration-200 ease-out hover:border-white/25 hover:bg-white/10"
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Back
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={handleNext}
                    className="flex min-h-[52px] flex-1 items-center justify-center rounded-2xl bg-lime-400 px-5 text-sm font-semibold text-emerald-950 shadow-[0_0_20px_-4px_rgba(132,204,22,0.6)] transition-all duration-200 ease-out hover:bg-lime-300 hover:shadow-[0_0_28px_-4px_rgba(132,204,22,0.85)] active:scale-[0.98]"
                  >
                    {step === 3 ? "Continue to Confirm" : "Next"}
                  </button>
                </div>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export default BookingModal;
