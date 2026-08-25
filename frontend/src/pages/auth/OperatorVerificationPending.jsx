import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  CheckCircle2,
  Clock,
  FileCheck,
  HardHat,
  Home,
  LogIn,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
  UserCheck,
} from 'lucide-react';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';

function OperatorVerificationPending() {
  const location = useLocation();

  const regData = location.state || (() => {
    const saved = sessionStorage.getItem('agro_pending_operator_reg');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return null;
      }
    }
    return null;
  })();

  const fullName = regData?.fullName || 'Operator';
  const mobileNumber = regData?.mobileNumber || '';

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem className="text-center sm:text-left">
        <div className="inline-flex items-center gap-2 rounded-full bg-amber-100 px-3.5 py-1 text-xs font-bold text-amber-900 mb-3">
          <Clock className="h-4 w-4 text-amber-700" />
          Status: Verification Pending
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Registration Submitted Successfully
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Thank you, <strong className="text-slate-800 font-semibold">{fullName}</strong>! Your machinery operator application is now under administrative review.
        </p>
      </RevealItem>

      {/* Verification Milestone Checklist */}
      <RevealItem className="mt-7 space-y-3">
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-emerald-700 text-white">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-emerald-950">Operator Account Created</h4>
              <p className="text-xs text-emerald-800/80">Primary profile & machinery skills recorded</p>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-emerald-700 text-white">
              <Smartphone className="h-5 w-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-emerald-950">Mobile Number Verified</h4>
              <p className="text-xs text-emerald-800/80">
                Authenticated via OTP confirmation ({mobileNumber || 'Registered Mobile'})
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-emerald-700 text-white">
              <FileCheck className="h-5 w-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-emerald-950">KYC & License Documents Submitted</h4>
              <p className="text-xs text-emerald-800/80">Aadhaar card and commercial driving license queued for verification</p>
            </div>
          </div>
        </div>
      </RevealItem>

      {/* Review Explanation Banner */}
      <RevealItem className="mt-6 rounded-2xl border border-amber-200 bg-amber-50/80 p-4 text-xs text-amber-900 space-y-1.5">
        <div className="flex items-center gap-2 font-bold text-amber-950 text-sm">
          <ShieldAlert className="h-4 w-4 text-amber-700" />
          What happens next?
        </div>
        <p className="text-amber-900/90 leading-relaxed">
          An administrator or partner fleet coordinator will review your government driving license and Aadhaar credentials.
          Verification is typically completed within <strong>1 to 2 business hours</strong>. You can sign in to your Operator account as soon as approval is granted.
        </p>
      </RevealItem>

      {/* Action Buttons */}
      <RevealItem className="mt-8 space-y-3">
        <Link to="/login/operator" className="block w-full">
          <MagneticButton className="w-full">
            <motion.div
              animate={{
                boxShadow: [
                  '0 0 20px 0px rgba(163,230,53,0.35)',
                  '0 0 38px 6px rgba(163,230,53,0.6)',
                  '0 0 20px 0px rgba(163,230,53,0.35)',
                ],
              }}
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
              className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98]"
            >
              <LogIn className="h-4 w-4" />
              Go to Operator Login
              <ArrowRight className="h-4 w-4" />
            </motion.div>
          </MagneticButton>
        </Link>

        <Link
          to="/"
          className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl border border-slate-300 bg-white text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
        >
          <Home className="h-4 w-4 text-slate-500" />
          Back to Home
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default OperatorVerificationPending;
