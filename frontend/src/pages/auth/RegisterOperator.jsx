import { Link } from 'react-router-dom';
import { ArrowRight, Sparkles, Users } from 'lucide-react';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';

function RegisterOperator() {
  return (
    <RevealGroup stagger={0.08} delayChildren={0.05}>
      <RevealItem className="flex justify-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-100 bg-emerald-50 text-emerald-700">
          <Users className="h-6 w-6" />
        </span>
      </RevealItem>

      <RevealItem className="mt-5 text-center">
        <h1 className="font-display text-[26px] font-bold tracking-tight text-slate-900 sm:text-[30px]">
          Operator Sign-Up
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">Coming soon.</p>
      </RevealItem>

      <RevealItem className="mt-6">
        <div className="rounded-2xl border border-slate-200 bg-[#F7F6F0] p-5 text-center">
          <p className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-emerald-700">
            <Sparkles className="h-3.5 w-3.5" />
            In progress
          </p>
          <p className="mt-2 text-sm leading-relaxed text-slate-600">
            Operator self-registration — with Aadhaar &amp; license verification, live job matching, and fast
            payouts — is being built. In the meantime, equipment owners can assign operators directly from their
            partner dashboard.
          </p>
        </div>
      </RevealItem>

      <RevealItem className="mt-6 space-y-3">
        <Link
          to="/login"
          className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98]"
        >
          Log In Instead
          <ArrowRight className="h-4 w-4" />
        </Link>
        <Link
          to="/register"
          className="flex min-h-[54px] w-full items-center justify-center rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-emerald-300 hover:bg-emerald-50"
        >
          Register as a Farmer Instead
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default RegisterOperator;
