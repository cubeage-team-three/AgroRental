import { motion } from 'framer-motion';
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
    <motion.button
      type={type}
      disabled={loading || disabled}
      onClick={onClick}
      animate={
        loading || disabled
          ? {}
          : {
              boxShadow: [
                '0 0 20px 0px rgba(163,230,53,0.30)',
                '0 0 36px 6px rgba(163,230,53,0.55)',
                '0 0 20px 0px rgba(163,230,53,0.30)',
              ],
            }
      }
      transition={{ duration: 2.4, repeat: Infinity, ease: 'easeInOut' }}
      className={`flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 px-6 py-3.5 text-[15px] font-semibold text-white shadow-md transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-70 ${className}`}
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
          {Icon && <Icon className="h-4 w-4" aria-hidden="true" />}
        </>
      )}
    </motion.button>
  );

  if (withMagnetic) {
    return <MagneticButton className="block w-full pt-1">{content}</MagneticButton>;
  }

  return <div className="block w-full pt-1">{content}</div>;
}

export default AuthPrimaryButton;
