import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { AlertCircle, CheckCircle2, Loader2, Star, Tractor, X } from "lucide-react";
import MagneticButton from "../ui/MagneticButton";
import { reviewService } from "../../services/reviewService";
import { getFarmerId } from "../../services/authService";

const EASE = [0.22, 1, 0.36, 1];
const MAX_COMMENT_LENGTH = 1000;

const RATING_LABELS = {
  1: "Poor",
  2: "Fair",
  3: "Good",
  4: "Great",
  5: "Excellent!",
};

function StarRating({ rating, hoverRating, onRate, onHover, onHoverEnd }) {
  return (
    <div className="flex items-center justify-center gap-2">
      {[1, 2, 3, 4, 5].map((star) => {
        const active = star <= (hoverRating || rating);
        return (
          <motion.button
            key={star}
            type="button"
            onClick={() => onRate(star)}
            onMouseEnter={() => onHover(star)}
            onMouseLeave={onHoverEnd}
            whileHover={{ scale: 1.18 }}
            whileTap={{ scale: 0.9 }}
            aria-label={`Rate ${star} star${star > 1 ? "s" : ""}`}
            className="p-1"
          >
            <Star
              className={`h-9 w-9 transition-colors duration-200 ${
                active
                  ? "fill-amber-400 text-amber-400 drop-shadow-[0_0_14px_rgba(251,191,36,0.65)]"
                  : "text-white/25"
              }`}
            />
          </motion.button>
        );
      })}
    </div>
  );
}

function ReviewSubmitModal({ isOpen, onClose, booking, onSubmitted }) {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [status, setStatus] = useState("form");
  const closeTimerRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setRating(0);
      setHoverRating(0);
      setComment("");
      setIsSubmitting(false);
      setSubmitError(null);
      setStatus("form");
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

  function handleClose() {
    if (isSubmitting) return;
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    onClose?.();
  }

  async function submitReview() {
    if (!rating) {
      setSubmitError("Please select a star rating before submitting.");
      return;
    }
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const review = await reviewService.createReview({
        bookingId: booking.id,
        farmerId: getFarmerId(),
        rating,
        comment: comment.trim() || undefined,
      });
      setStatus("success");
      onSubmitted?.(review);
      closeTimerRef.current = setTimeout(() => {
        onClose?.();
      }, 2200);
    } catch (err) {
      setSubmitError(err.message || "Failed to submit review. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

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
            className="relative flex max-h-[92vh] w-full max-w-[440px] flex-col overflow-hidden rounded-3xl border border-white/15 bg-emerald-950/80 shadow-[0_30px_90px_-20px_rgba(0,0,0,0.6)] backdrop-blur-2xl"
          >
            <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_20%_0%,rgba(16,185,129,0.25),transparent_55%)]" />
            <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_85%_100%,rgba(132,204,22,0.16),transparent_55%)]" />

            <button
              type="button"
              onClick={handleClose}
              aria-label="Close review dialog"
              className="absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/60 backdrop-blur-md transition-all duration-200 ease-out hover:border-white/25 hover:bg-white/10 hover:text-white"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="overflow-y-auto px-6 pb-6 pt-6">
              {status === "success" ? (
                <div className="flex flex-col items-center py-6 text-center">
                  <motion.span
                    initial={{ scale: 0.4, opacity: 0, rotate: -20 }}
                    animate={{ scale: 1, opacity: 1, rotate: 0 }}
                    transition={{ type: "spring", stiffness: 260, damping: 18 }}
                    className="flex h-16 w-16 items-center justify-center rounded-full bg-lime-400/15 text-lime-300 shadow-[0_0_35px_-4px_rgba(163,230,53,0.7)]"
                  >
                    <CheckCircle2 className="h-9 w-9" />
                  </motion.span>
                  <h3 className="mt-5 font-display text-2xl font-bold text-white">Review Submitted!</h3>
                  <p className="mt-2 max-w-[280px] text-sm leading-relaxed text-white/60">
                    Thank you for sharing your experience with the AgroRent community.
                  </p>
                  <div className="mt-4 flex items-center gap-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <Star
                        key={star}
                        className={`h-5 w-5 ${
                          star <= rating ? "fill-amber-400 text-amber-400" : "text-white/20"
                        }`}
                      />
                    ))}
                  </div>
                  {comment && (
                    <p className="mt-3 max-w-[300px] text-sm italic leading-relaxed text-white/50">
                      &ldquo;{comment}&rdquo;
                    </p>
                  )}
                  <button
                    type="button"
                    onClick={handleClose}
                    className="mt-6 flex min-h-[52px] w-full items-center justify-center rounded-2xl border border-white/15 bg-white/5 text-sm font-semibold text-white backdrop-blur-md transition-all duration-200 ease-out hover:border-white/25 hover:bg-white/10"
                  >
                    Done
                  </button>
                </div>
              ) : (
                <>
                  <div className="flex items-center gap-3 pr-8">
                    <span className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/30 to-lime-400/20 shadow-lg">
                      <Tractor className="h-6 w-6 text-lime-300" />
                    </span>
                    <div className="min-w-0">
                      <p className="truncate text-[10px] font-semibold uppercase tracking-widest text-lime-300/80">
                        Rate Your Experience
                      </p>
                      <h2 className="truncate font-display text-base font-bold text-white sm:text-lg">
                        {booking?.equipmentName || "Rental Service"}
                      </h2>
                      {booking?.id && (
                        <p className="mt-0.5 truncate text-xs text-white/50">Booking #{booking.id}</p>
                      )}
                    </div>
                  </div>

                  <div className="mt-7 text-center">
                    <StarRating
                      rating={rating}
                      hoverRating={hoverRating}
                      onRate={setRating}
                      onHover={setHoverRating}
                      onHoverEnd={() => setHoverRating(0)}
                    />
                    <p className="mt-3 text-sm font-semibold text-lime-300/90">
                      {RATING_LABELS[hoverRating || rating] || "Tap a star to rate"}
                    </p>
                  </div>

                  <div className="mt-6">
                    <label
                      htmlFor="review-comment"
                      className="mb-2 block text-xs font-semibold uppercase tracking-wide text-white/50"
                    >
                      Share your experience (optional)
                    </label>
                    <div className="rounded-2xl border border-white/15 bg-white/5 backdrop-blur-md transition-all duration-300 ease-out focus-within:border-lime-300/70 focus-within:bg-white/10 focus-within:shadow-[0_0_0_4px_rgba(163,230,53,0.15),0_0_28px_-6px_rgba(163,230,53,0.55)]">
                      <textarea
                        id="review-comment"
                        rows={4}
                        maxLength={MAX_COMMENT_LENGTH}
                        value={comment}
                        onChange={(e) => setComment(e.target.value)}
                        placeholder="Tell other farmers about the equipment, the operator, timeliness…"
                        className="w-full resize-none rounded-2xl bg-transparent px-4 py-3.5 text-[15px] text-white placeholder-white/30 outline-none"
                      />
                    </div>
                    <div className="mt-1.5 text-right text-[11px] text-white/30">
                      {comment.length}/{MAX_COMMENT_LENGTH}
                    </div>
                  </div>

                  {submitError && (
                    <div className="mt-1 flex items-start gap-2 rounded-xl border border-red-400/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                      <span>{submitError}</span>
                    </div>
                  )}

                  <MagneticButton className="mt-5 block w-full">
                    <motion.button
                      type="button"
                      onClick={submitReview}
                      disabled={isSubmitting}
                      animate={
                        isSubmitting
                          ? {}
                          : {
                              boxShadow: [
                                "0 0 20px 0px rgba(163,230,53,0.35)",
                                "0 0 38px 6px rgba(163,230,53,0.6)",
                                "0 0 20px 0px rgba(163,230,53,0.35)",
                              ],
                            }
                      }
                      transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
                      className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-lime-400 px-6 text-[15px] font-semibold text-emerald-950 transition-transform duration-200 ease-out active:scale-[0.98] disabled:opacity-70"
                    >
                      {isSubmitting ? (
                        <>
                          <Loader2 className="h-[18px] w-[18px] animate-spin" />
                          Submitting...
                        </>
                      ) : (
                        "Submit Review"
                      )}
                    </motion.button>
                  </MagneticButton>
                </>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export default ReviewSubmitModal;
