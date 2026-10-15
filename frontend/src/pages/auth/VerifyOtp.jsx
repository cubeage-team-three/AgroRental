import { Navigate, useLocation } from 'react-router-dom';

/**
 * Farmer OTP verification is no longer part of registration or login: farmers sign in with their mobile
 * number (or email) and password. Old links and bookmarks to this page are forwarded to the farmer login.
 */
function VerifyOtp() {
  const location = useLocation();
  const mobileNumber = location.state?.mobileNumber || '';

  return (
    <Navigate
      to="/login?role=FARMER"
      replace
      state={{
        role: 'FARMER',
        mobileOrEmail: mobileNumber,
        message:
          'OTP verification is no longer needed. Log in with your mobile number (or email) and password. ' +
          'If you never set a password, use "Forgot password?".',
      }}
    />
  );
}

export default VerifyOtp;
