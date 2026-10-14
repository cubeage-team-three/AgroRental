import { useState } from 'react';
import { Eye, EyeOff, Lock } from 'lucide-react';
import AuthTextField from './AuthTextField';

function AuthPasswordField({
  id = 'password',
  label = 'Password',
  name = 'password',
  value,
  onChange,
  error,
  icon = Lock,
  autoComplete = 'current-password',
  required = false,
  ...props
}) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <AuthTextField
      id={id}
      name={name}
      label={label}
      type={showPassword ? 'text' : 'password'}
      value={value}
      onChange={onChange}
      error={error}
      icon={icon}
      autoComplete={autoComplete}
      required={required}
      rightSlot={
        <button
          type="button"
          onClick={() => setShowPassword((prev) => !prev)}
          className="rounded-lg p-1 text-slate-400 transition-colors hover:text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
          aria-label={showPassword ? 'Hide password' : 'Show password'}
        >
          {showPassword ? (
            <EyeOff className="h-[18px] w-[18px]" aria-hidden="true" />
          ) : (
            <Eye className="h-[18px] w-[18px]" aria-hidden="true" />
          )}
        </button>
      }
      {...props}
    />
  );
}

export default AuthPasswordField;
