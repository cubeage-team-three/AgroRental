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
      className={`flex rounded-2xl bg-[#F0EFE9] p-1 ${className}`}
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
            className={`flex-1 rounded-xl py-2.5 text-sm font-semibold transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${
              isActive
                ? 'bg-white text-emerald-800 shadow-sm font-bold'
                : 'text-slate-500 hover:text-slate-800 font-medium'
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
