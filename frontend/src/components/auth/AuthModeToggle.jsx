function AuthModeToggle({
  modes = [
    { id: 'PASSWORD', label: 'Password' },
    { id: 'OTP', label: 'OTP Login' },
  ],
  activeMode,
  onChange,
  className = '',
}) {
  return (
    <div
      role="tablist"
      aria-label="Authentication method"
      className={`flex rounded-xl border border-slate-200/80 bg-slate-100/80 p-1 ${className}`}
    >
      {modes.map((mode) => {
        const isActive = activeMode === mode.id;
        return (
          <button
            key={mode.id}
            role="tab"
            aria-selected={isActive}
            type="button"
            onClick={() => onChange(mode.id)}
            className={`flex-1 rounded-lg py-2.5 text-sm transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${
              isActive
                ? 'bg-white font-semibold text-emerald-800 shadow-sm ring-1 ring-slate-900/5'
                : 'font-medium text-slate-500 hover:bg-white/60 hover:text-slate-800'
            }`}
          >
            {mode.label}
          </button>
        );
      })}
    </div>
  );
}

export default AuthModeToggle;
