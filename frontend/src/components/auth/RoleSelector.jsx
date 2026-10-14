import { motion } from 'framer-motion';
import { HardHat, LayoutDashboard, Sprout, Tractor } from 'lucide-react';

export const AUTH_ROLES = [
  {
    id: 'farmer',
    roleCode: 'FARMER',
    label: 'Farmer',
    icon: Sprout,
    path: '/register?role=farmer',
    description: 'Rent & book equipment',
  },
  {
    id: 'partner',
    altId: 'owner',
    roleCode: 'PARTNER',
    label: 'Equipment Owner',
    icon: Tractor,
    path: '/register?role=partner',
    description: 'List machinery fleet',
  },
  {
    id: 'operator',
    roleCode: 'OPERATOR',
    label: 'Machinery Operator',
    icon: HardHat,
    path: '/register?role=operator',
    description: 'Execute field operations',
  },
  {
    id: 'admin',
    roleCode: 'ADMIN',
    label: 'Admin',
    icon: LayoutDashboard,
    path: '/admin/login',
    description: 'Platform management',
  },
];

function RoleSelector({
  selectedRole = 'farmer',
  onSelectRole,
  showAdmin = false,
  label = 'I am a',
  className = '',
}) {
  const visibleRoles = showAdmin
    ? AUTH_ROLES
    : AUTH_ROLES.filter((r) => r.id !== 'admin');

  // Normalize selectedRole (owner maps to partner)
  const normalizedSelected =
    selectedRole === 'owner' ? 'partner' : selectedRole;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <span className="mb-2.5 block text-xs font-semibold uppercase tracking-wider text-slate-500">
          {label}
        </span>
      )}

      <div
        role="radiogroup"
        aria-label="Select user role"
        className={`grid ${
          visibleRoles.length === 4 ? 'grid-cols-2 sm:grid-cols-4' : 'grid-cols-3'
        } gap-2.5 sm:gap-3`}
      >
        {visibleRoles.map((role) => {
          const isSelected =
            normalizedSelected === role.id || selectedRole === role.id;
          const Icon = role.icon;

          return (
            <button
              key={role.id}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => onSelectRole?.(role.id, role.path)}
              className={`group relative flex min-h-[82px] flex-col items-center justify-center gap-1.5 rounded-2xl border-2 p-2.5 sm:p-3 text-center transition-all duration-200 outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 focus-visible:ring-offset-2 ${
                isSelected
                  ? 'border-emerald-600 bg-emerald-50/80 text-emerald-950 shadow-sm ring-2 ring-emerald-600/10'
                  : 'border-slate-200/90 bg-white text-slate-600 hover:border-emerald-300 hover:bg-emerald-50/30'
              }`}
            >
              {isSelected && (
                <motion.span
                  layoutId="auth-role-highlight"
                  transition={{ type: 'spring', stiffness: 440, damping: 32 }}
                  className="pointer-events-none absolute inset-0 rounded-2xl border-2 border-emerald-600 bg-emerald-50/60"
                  aria-hidden="true"
                />
              )}

              <Icon
                className={`relative z-10 h-5 w-5 shrink-0 transition-colors duration-200 ${
                  isSelected
                    ? 'text-emerald-700'
                    : 'text-slate-400 group-hover:text-emerald-600'
                }`}
                aria-hidden="true"
              />

              <span
                className={`relative z-10 text-xs font-semibold leading-tight transition-colors duration-200 ${
                  isSelected
                    ? 'text-emerald-950 font-bold'
                    : 'text-slate-600 group-hover:text-slate-900'
                }`}
              >
                {role.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default RoleSelector;
