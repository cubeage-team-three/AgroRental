import { ArrowRight, Loader2 } from 'lucide-react';
import MagneticButton from '../ui/MagneticButton';

function AuthPrimaryButton({
  children,
  loading = false,
  loadingText,
  icon: Icon = ArrowRight,
  disabled = false,
  type = 'submit',
  onClick,
  className = '',
  withMagnetic = true,
  ...props
}) {
  const content = (
    <button
      type={type}
      disabled={loading || disabled}
      aria-busy={loading || undefined}
      onClick={onClick}
      className={`group flex min-h-[54px] w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-6 py-3.5 text-[15px] font-semibold tracking-[0.01em] text-white shadow-[0_1px_2px_rgba(6,78,59,0.25),0_6px_16px_-6px_rgba(6,95,70,0.55)] transition-all duration-200 ease-out hover:bg-emerald-800 hover:shadow-[0_1px_2px_rgba(6,78,59,0.3),0_10px_22px_-8px_rgba(6,95,70,0.6)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 focus-visible:ring-offset-2 active:scale-[0.99] disabled:cursor-not-allowed disabled:bg-emerald-700/60 disabled:shadow-none disabled:active:scale-100 ${className}`}
      {...props}
    >
      {loading ? (
        <>
          <Loader2 className="h-[18px] w-[18px] animate-spin" aria-hidden="true" />
          <span>{loadingText || children}</span>
        </>
      ) : (
        <>
          <span>{children}</span>
          {Icon && (
            <Icon
              className="h-4 w-4 transition-transform duration-200 ease-out group-hover:translate-x-0.5 group-disabled:translate-x-0"
              aria-hidden="true"
            />
          )}
        </>
      )}
    </button>
  );

  if (withMagnetic) {
    return <MagneticButton className="block w-full pt-1">{content}</MagneticButton>;
  }

  return <div className="block w-full pt-1">{content}</div>;
}

export default AuthPrimaryButton;
