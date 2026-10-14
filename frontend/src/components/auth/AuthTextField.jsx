import { useState } from 'react';

function AuthTextField({
  id,
  label,
  name,
  type = 'text',
  value,
  onChange,
  icon: Icon,
  error,
  rightSlot,
  prefix,
  className = '',
  required = false,
  autoComplete,
  ...props
}) {
  const [focused, setFocused] = useState(false);
  const hasValue = value !== undefined && value !== '' && value !== null;
  const active = focused || hasValue;

  return (
    <div className={`w-full ${className}`}>
      <div
        className={`relative flex items-stretch rounded-2xl border transition-all duration-300 ease-out ${
          error
            ? 'border-red-400 bg-red-50/60 shadow-[0_0_0_3px_rgba(239,68,68,0.12)]'
            : focused
              ? 'border-emerald-600 bg-white shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.45)]'
              : 'border-slate-200/80 bg-[#F7F6F0] hover:border-emerald-300/80'
        }`}
      >
        {prefix && (
          <span className="flex shrink-0 items-center border-r border-slate-300/70 pl-4 pr-3 text-sm font-semibold text-slate-600 select-none">
            {prefix}
          </span>
        )}

        <div className="relative flex-1">
          {Icon && (
            <Icon
              className={`pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 transition-colors duration-200 ${
                focused ? 'text-emerald-700' : 'text-slate-400'
              }`}
              aria-hidden="true"
            />
          )}

          <input
            id={id}
            name={name || id}
            type={type}
            value={value}
            onChange={onChange}
            required={required}
            autoComplete={autoComplete}
            onFocus={(event) => {
              setFocused(true);
              props.onFocus?.(event);
            }}
            onBlur={(event) => {
              setFocused(false);
              props.onBlur?.(event);
            }}
            className={`peer min-h-[56px] w-full rounded-2xl bg-transparent px-4 pt-5 pb-1.5 text-[15px] font-medium text-slate-900 outline-none transition-colors ${
              Icon ? 'pl-11' : ''
            } ${rightSlot ? 'pr-11' : ''}`}
            {...props}
          />

          {label && (
            <label
              htmlFor={id}
              className={`pointer-events-none absolute transition-all duration-200 ease-out select-none ${
                Icon ? 'left-11' : 'left-4'
              } ${
                active
                  ? 'top-2 text-[11px] font-bold tracking-wide text-emerald-800'
                  : 'top-1/2 -translate-y-1/2 text-[14px] font-normal text-slate-500'
              }`}
            >
              {label}
              {required && <span className="ml-0.5 text-red-500">*</span>}
            </label>
          )}

          {rightSlot && (
            <div className="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center">
              {rightSlot}
            </div>
          )}
        </div>
      </div>

      {error && (
        <p className="mt-1.5 pl-1 text-xs font-semibold text-red-600 animate-in fade-in duration-200">
          {error}
        </p>
      )}
    </div>
  );
}

export default AuthTextField;
