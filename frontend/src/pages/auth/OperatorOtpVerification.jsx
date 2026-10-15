import { Navigate } from 'react-router-dom';
import { getOnboardingToken, getPendingRegistration } from './operatorOnboardingSession';

/**
 * Operator mobile OTP verification is no longer part of registration (accounts are verified by an
 * administrator). This former route only forwards: to the KYC upload step while a registration is in
 * progress in this tab, otherwise to the operator login.
 */
function OperatorOtpVerification() {
  const registration = getPendingRegistration();
  if (registration?.operatorId && getOnboardingToken()) {
    return <Navigate to="/register/operator/kyc" replace />;
  }
  return <Navigate to="/login?role=OPERATOR" replace />;
}

export default OperatorOtpVerification;
