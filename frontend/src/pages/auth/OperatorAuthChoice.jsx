import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  HardHat,
  LayoutDashboard,
  LogIn,
  Sprout,
  Tractor,
  UserPlus,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';

const ROLES = [
  { id: 'farmer', label: 'Farmer', icon: Sprout },
  { id: 'owner', label: 'Equipment Owner', icon: Tractor },
  { id: 'operator', label: 'Operator', icon: HardHat },
  { id: 'admin', label: 'Admin', icon: LayoutDashboard },
];

function OperatorAuthChoice() {
  const navigate = useNavigate();

  const handleRoleSelect = (roleId) => {
    if (roleId === 'farmer') {
      navigate('/register');
      return;
    }
    if (roleId === 'owner') {
      navigate('/register/partner');
      return;
    }
    if (roleId === 'admin') {
      navigate('/admin/login');
      return;
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Machinery Operator Portal
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Get deployed on verified farm tasks, track fieldwork hours, and receive guaranteed payouts.
        </p>
      </RevealItem>

      <RevealItem className="mt-7">
        <span className="mb-3 block text-xs font-semibold uppercase tracking-wide text-slate-400">
          I am a
        </span>
        <div className="grid grid-cols-4 gap-2">
          {ROLES.map((r) => {
            const active = r.id === 'operator';
            return (
              <button
                key={r.id}
                type="button"
                onClick={() => handleRoleSelect(r.id)}
                className="relative flex flex-col items-center justify-center gap-1.5 rounded-2xl border-2 border-transparent p-3 transition-colors duration-200"
              >
                {active && (
                  <motion.span
                    layoutId="operator-role-highlight"
                    transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                    className="absolute inset-0 rounded-2xl border-2 border-emerald-600 bg-emerald-50"
                  />
                )}
                <r.icon className={`relative h-5 w-5 ${active ? 'text-emerald-700' : 'text-slate-400'}`} />
                <span className={`relative text-[11px] font-semibold ${active ? 'text-emerald-800' : 'text-slate-500'}`}>
                  {r.label}
                </span>
              </button>
            );
          })}
        </div>
      </RevealItem>

      {/* Two Clear Choices: Login or Register */}
      <RevealItem className="mt-8 space-y-4">
        {/* Option 1: Login */}
        <Link
          to="/login/operator"
          className="group block w-full rounded-2xl border-2 border-emerald-700/20 bg-white p-5 shadow-sm transition-all duration-200 hover:border-emerald-600 hover:shadow-md active:scale-[0.99]"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-800 transition-colors group-hover:bg-emerald-800 group-hover:text-white">
                <LogIn className="h-6 w-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-800">
                  Login to Operator Account
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Already registered? Access your assigned jobs & earnings
                </p>
              </div>
            </div>
            <ArrowRight className="h-5 w-5 text-slate-400 transition-transform group-hover:translate-x-1 group-hover:text-emerald-700" />
          </div>
        </Link>

        {/* Option 2: Register */}
        <Link
          to="/register/operator"
          className="group block w-full rounded-2xl border-2 border-emerald-600 bg-emerald-50 p-5 shadow-sm transition-all duration-200 hover:bg-emerald-100/70 hover:shadow-md active:scale-[0.99]"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-800 text-white">
                <UserPlus className="h-6 w-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold text-emerald-950">
                    Create Operator Account
                  </h3>
                  <span className="rounded-full bg-emerald-200 px-2 py-0.5 text-[10px] font-bold text-emerald-900">
                    New
                  </span>
                </div>
                <p className="text-xs text-emerald-800/80 mt-0.5">
                  Register with your Driving License & Aadhaar to start working
                </p>
              </div>
            </div>
            <ArrowRight className="h-5 w-5 text-emerald-700 transition-transform group-hover:translate-x-1" />
          </div>
        </Link>
      </RevealItem>

      <RevealItem className="mt-8 rounded-2xl bg-slate-50 p-4 border border-slate-200/70 text-xs text-slate-600 space-y-2">
        <div className="flex items-center gap-2 font-semibold text-slate-800">
          <ShieldCheck className="h-4 w-4 text-emerald-600" />
          Why join AgroRent as a Certified Operator?
        </div>
        <ul className="space-y-1 pl-6 list-disc text-slate-500 text-[11px]">
          <li>Guaranteed hourly/daily compensation directly to your bank account</li>
          <li>Direct deployment with verified equipment owners & tractor fleets</li>
          <li>Accurate fieldwork duration tracking with server-validated timestamps</li>
        </ul>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500">
        <Link to="/" className="font-semibold text-slate-600 hover:text-emerald-700 transition-colors">
          ← Back to Home
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default OperatorAuthChoice;
