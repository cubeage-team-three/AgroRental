import { useState } from 'react';
import { useReducedMotion } from 'framer-motion';
import { Pause, Play } from 'lucide-react';

/**
 * Scrolling announcement bar shown directly below the public Navbar.
 *
 * Informational only: it links out to the official PM-KISAN portal and never
 * collects Aadhaar, OTP, bank or any other personal data inside AgroRent.
 *
 * Motion: a CSS marquee (right → left) that pauses on hover, while anything in
 * the bar has keyboard focus, or via the pause button. With "reduce motion"
 * enabled at the OS level the message is rendered static and wrapped instead.
 */

const PM_KISAN_URL = 'https://pmkisan.gov.in/';

const MESSAGE =
  '📢 शेतकरी बांधवांसाठी महत्त्वाची सूचना! PM-KISAN योजनेतील नोंदणीकृत लाभार्थ्यांसाठी e-KYC अनिवार्य आहे. e-KYC, लाभार्थी स्थिती व योजनेच्या माहितीसाठी अधिकृत सरकारी पोर्टलला भेट द्या.';

// The track is two identical halves animated from 0 to -50%, so the loop is
// seamless. Each half repeats the message twice so it stays wider than even
// very wide viewports (no blank gap before the loop restarts).
const COPIES = 4;

function PortalLink({ className = '' }) {
  return (
    <a
      href={PM_KISAN_URL}
      target="_blank"
      rel="noopener noreferrer"
      lang="mr"
      className={`inline-flex min-h-[44px] shrink-0 items-center gap-1 rounded-xl bg-lime-400 px-3.5 py-2 text-xs font-semibold text-emerald-950 shadow-[0_0_15px_rgba(132,204,22,0.35)] transition-all duration-300 ease-out hover:bg-lime-300 hover:shadow-[0_0_20px_rgba(132,204,22,0.6)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-lime-300 sm:text-sm ${className}`}
    >
      PM-KISAN अधिकृत पोर्टल <span aria-hidden="true">↗</span>
      <span className="sr-only"> (नवीन टॅबमध्ये उघडते)</span>
    </a>
  );
}

function NewsTicker() {
  const reduceMotion = useReducedMotion();
  const [isPaused, setIsPaused] = useState(false);

  return (
    <section
      aria-label="PM-KISAN सूचना"
      className="group border-b border-emerald-900/40 bg-emerald-950 text-emerald-50"
    >
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-3 gap-y-1.5 px-4 py-1.5 sm:flex-nowrap sm:px-6">
        <span className="hidden shrink-0 items-center rounded-full bg-lime-400/15 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-lime-300 ring-1 ring-lime-400/30 md:inline-flex">
          PM-KISAN
        </span>

        {reduceMotion ? (
          <p lang="mr" className="basis-full py-1 text-sm leading-relaxed sm:flex-1 sm:basis-auto">
            {MESSAGE}
          </p>
        ) : (
          <div className="relative order-first basis-full overflow-hidden [mask-image:linear-gradient(to_right,transparent,black_24px,black_calc(100%-24px),transparent)] sm:order-none sm:flex-1 sm:basis-auto">
            <div
              role="marquee"
              className={`flex w-max animate-news-ticker motion-reduce:animate-none group-hover:[animation-play-state:paused] group-focus-within:[animation-play-state:paused] ${isPaused ? '[animation-play-state:paused]' : ''}`}
            >
              {Array.from({ length: COPIES }, (_, i) => (
                <p
                  key={i}
                  lang="mr"
                  aria-hidden={i > 0 ? 'true' : undefined}
                  className="shrink-0 whitespace-nowrap py-1.5 pr-16 text-sm font-medium"
                >
                  {MESSAGE}
                </p>
              ))}
            </div>
          </div>
        )}

        <div className="ml-auto flex shrink-0 items-center gap-2 sm:ml-0">
          {!reduceMotion && (
            <button
              type="button"
              onClick={() => setIsPaused((paused) => !paused)}
              aria-pressed={isPaused}
              aria-label={isPaused ? 'Resume announcement' : 'Pause announcement'}
              className="flex h-11 w-11 items-center justify-center rounded-xl border border-emerald-700/60 text-emerald-100 transition-all duration-300 ease-out hover:border-lime-400/60 hover:text-lime-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-lime-300"
            >
              {isPaused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
            </button>
          )}
          <PortalLink />
        </div>
      </div>
    </section>
  );
}

export default NewsTicker;
