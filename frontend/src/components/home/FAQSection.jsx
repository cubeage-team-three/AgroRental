import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Plus, MessageCircle } from "lucide-react";

const faqs = [
  {
    question: "How do I book if I don't have a smartphone?",
    answer:
      "Send a WhatsApp message describing your service, crop, and field location. Our support agents create the booking on your behalf, confirm it with an OTP, and send you a browser-based tracking link — no app or login required.",
  },
  {
    question: "How is the price calculated?",
    answer:
      "Pricing depends on the service: per acre for ploughing, seeding, and spraying; per hour for irrigation setup and custom operators; per machine for harvester bookings; and per job for one-off visits like soil testing. A minimum charge and travel surcharge may apply for small acreage or long-distance jobs.",
  },
  {
    question: "Are operators verified?",
    answer:
      "Yes. Every operator's Aadhaar and driving license are verified, and their profile is approved by our admin team before they can accept any bookings.",
  },
  {
    question: "Can I track the machine to my field?",
    answer:
      "Yes. Once an operator is assigned, you get live GPS tracking with an ETA, so you can see exactly when your equipment and operator will arrive.",
  },
];

function FAQSection() {
  const [openIndex, setOpenIndex] = useState(null);

  const toggle = (index) => {
    setOpenIndex((current) => (current === index ? null : index));
  };

  return (
    <section className="relative isolate overflow-hidden bg-slate-50">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(16,185,129,0.10),transparent_55%)]" />

      <div className="relative mx-auto max-w-3xl px-6 py-24">
        <div className="text-center">
          <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-emerald-700">
            FAQ
          </span>
          <h2 className="mt-5 font-display text-4xl font-bold tracking-tight text-emerald-950 sm:text-5xl">
            Common Questions
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-slate-600">
            Everything farmers ask us before their first booking.
          </p>
        </div>

        <div className="mt-12 space-y-4">
          {faqs.map((faq, index) => {
            const isOpen = openIndex === index;
            return (
              <div
                key={faq.question}
                className={`group overflow-hidden rounded-2xl border bg-white shadow-lg transition-all duration-300 ease-out ${
                  isOpen
                    ? "border-emerald-300 shadow-xl shadow-emerald-900/10"
                    : "border-slate-200/70 shadow-emerald-900/5 hover:-translate-y-1 hover:border-emerald-200 hover:shadow-xl"
                }`}
              >
                <button
                  type="button"
                  onClick={() => toggle(index)}
                  aria-expanded={isOpen}
                  className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left transition-all duration-300 ease-out"
                >
                  <span
                    className={`font-semibold transition-colors duration-300 ease-out ${
                      isOpen ? "text-emerald-800" : "text-emerald-950"
                    }`}
                  >
                    {faq.question}
                  </span>
                  <span
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full transition-all duration-300 ease-out ${
                      isOpen
                        ? "rotate-45 bg-emerald-600 text-white"
                        : "bg-emerald-50 text-emerald-700 group-hover:bg-emerald-100"
                    }`}
                  >
                    <Plus className="h-4 w-4" strokeWidth={2.5} />
                  </span>
                </button>

                <AnimatePresence initial={false}>
                  {isOpen && (
                    <motion.div
                      key="content"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.3, ease: "easeOut" }}
                      className="overflow-hidden"
                    >
                      <p className="border-t border-slate-100 px-6 py-5 text-sm leading-relaxed text-slate-600">
                        {faq.answer}
                      </p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>

        {/* Closing support prompt */}
        <div className="mt-10 flex flex-col items-center gap-4 rounded-2xl border border-slate-200/70 bg-white/70 px-8 py-6 text-center shadow-sm backdrop-blur-md sm:flex-row sm:justify-between sm:text-left">
          <div>
            <p className="font-semibold text-emerald-950">
              Still have questions?
            </p>
            <p className="mt-0.5 text-sm text-slate-600">
              Our support team answers on WhatsApp in your language.
            </p>
          </div>
          <a
            href="https://wa.me/919876543210"
            target="_blank"
            rel="noopener noreferrer"
            className="group inline-flex shrink-0 items-center gap-2 rounded-xl bg-lime-400 px-6 py-3 font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.45)] transition-all duration-300 ease-out hover:-translate-y-1 hover:bg-lime-300 hover:shadow-[0_0_30px_rgba(132,204,22,0.7)]"
          >
            <MessageCircle className="h-5 w-5" />
            Chat on WhatsApp
          </a>
        </div>
      </div>
    </section>
  );
}

export default FAQSection;
