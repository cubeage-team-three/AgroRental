import { useState } from 'react';
import { AlertCircle } from 'lucide-react';

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
        className={`relative flex items-stretch rounded-xl border transition-[border-color,box-shadow,background-color] duration-200 ease-out ${
          error
            ? 'border-red-300 bg-red-50/40 shadow-[0_0_0_3px_rgba(239,68,68,0.10)]'
            : focused
              ? 'border-emerald-600 bg-white shadow-[0_0_0_3px_rgba(5,150,105,0.14)]'
              : 'border-slate-200 bg-white shadow-xs hover:border-slate-300'
        }`}
      >
        {prefix && (
          <span className="flex shrink-0 items-center rounded-l-xl border-r border-slate-200 bg-slate-50 pl-4 pr-3 text-sm font-semibold text-slate-700 select-none">
            {prefix}
          </span>
        )}

        <div className="relative flex-1">
          {Icon && (
            <Icon
              className={`pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 transition-colors duration-200 ${
                error ? 'text-red-400' : focused ? 'text-emerald-700' : 'text-slate-400'
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
            className={`peer min-h-[54px] w-full rounded-xl bg-transparent px-4 pt-5 pb-1.5 text-[15px] font-medium text-slate-900 outline-none transition-colors placeholder:text-slate-400 ${
              Icon ? 'pl-11' : ''
            } ${rightSlot ? 'pr-11' : ''}`}
            {...props}
          />

          {label && (
            <label
              htmlFor={id}
              className={`pointer-events-none absolute truncate transition-all duration-200 ease-out select-none ${
                Icon ? 'left-11' : 'left-4'
              } ${rightSlot ? 'right-11' : 'right-4'} ${
                active
                  ? `top-2 text-[11px] font-semibold tracking-wide ${
                      error ? 'text-red-600' : focused ? 'text-emerald-700' : 'text-slate-500'
                    }`
                  : 'top-1/2 -translate-y-1/2 text-[14.5px] font-normal text-slate-500'
              }`}
            >
              {label}
              {required && <span className="ml-0.5 text-red-500/90">*</span>}
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
        <p className="mt-1.5 flex items-start gap-1.5 pl-1 text-xs font-medium text-red-600 animate-in fade-in duration-200">
          <AlertCircle className="mt-px h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </p>
      )}
    </div>
  );
}

export default AuthTextField;
