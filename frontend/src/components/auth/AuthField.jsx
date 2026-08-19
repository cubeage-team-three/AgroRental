import { useState } from "react";

function AuthField({ id, label, icon: Icon, error, rightSlot, prefix, className = "", ...props }) {
  const [focused, setFocused] = useState(false);
  const active = focused || (props.value !== undefined && props.value !== "" && props.value !== null);

  return (
    <div className={className}>
      <div
        className={`relative flex items-stretch rounded-2xl border bg-[#F7F6F0] transition-all duration-300 ease-out ${
          error
            ? "border-red-400 bg-red-50"
            : focused
              ? "border-emerald-500 bg-white shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.55)]"
              : "border-transparent hover:border-emerald-200"
        }`}
      >
        {prefix && (
          <span className="flex shrink-0 items-center border-r border-slate-300/70 pl-4 pr-3 text-sm font-semibold text-slate-500">
            {prefix}
          </span>
        )}
        <div className="relative flex-1">
          {Icon && (
            <Icon
              className={`pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 transition-colors duration-300 ${
                focused ? "text-emerald-600" : "text-slate-400"
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
            className={`peer min-h-[58px] w-full rounded-2xl bg-transparent px-4 pt-6 pb-2 text-[15px] text-slate-900 outline-none ${
              Icon ? "pl-11" : ""
            } ${rightSlot ? "pr-11" : ""}`}
            {...props}
          />
          <label
            htmlFor={id}
            className={`pointer-events-none absolute ${Icon ? "left-11" : "left-4"} transition-all duration-300 ease-out ${
              active
                ? "top-[9px] text-[11px] font-medium tracking-wide text-emerald-700"
                : "top-1/2 -translate-y-1/2 text-[15px] text-slate-400"
            }`}
          >
            {label}
          </label>
          {rightSlot && <div className="absolute right-3 top-1/2 -translate-y-1/2">{rightSlot}</div>}
        </div>
      </div>
      {error && <p className="mt-1.5 pl-1 text-xs font-medium text-red-500">{error}</p>}
    </div>
  );
}

export default AuthField;
