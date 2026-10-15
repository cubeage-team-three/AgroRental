/**
 * Client-side mirror of the backend password policy (com.agrorental.auth.service.PasswordPolicy):
 * 8-64 characters with at least one letter and one number. The server always re-checks; this only
 * gives faster feedback.
 */
export const PASSWORD_POLICY_HINT = '8-64 characters, with at least one letter and one number.';

export const PASSWORD_POLICY_MESSAGE =
  'Password must be 8-64 characters long and contain at least one letter and one number.';

const MAX_UTF8_BYTES = 72;

export function getPasswordPolicyError(password) {
  if (!password || !password.trim()) {
    return PASSWORD_POLICY_MESSAGE;
  }
  const length = Array.from(password).length;
  if (length < 8 || length > 64) {
    return PASSWORD_POLICY_MESSAGE;
  }
  if (!/\p{L}/u.test(password) || !/\p{Nd}/u.test(password)) {
    return PASSWORD_POLICY_MESSAGE;
  }
  if (new TextEncoder().encode(password).length > MAX_UTF8_BYTES) {
    return 'Password is too long. Please use fewer characters (non-English characters count more).';
  }
  return '';
}

export function getPasswordConfirmError(password, confirmPassword) {
  return password === confirmPassword ? '' : 'Password and Confirm Password do not match.';
}
