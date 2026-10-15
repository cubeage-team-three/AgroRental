/**
 * Registration-time state of a just-registered (PENDING) operator, kept in sessionStorage only: it lives
 * for this browser tab and is never written to localStorage.
 *
 * The onboarding token returned by POST /api/operators/register is NOT a login. The backend accepts it only
 * for uploading this operator's own KYC documents while the account awaits admin approval (at most 2 hours).
 */
const TOKEN_KEY = 'agro_operator_onboarding_token';
const REGISTRATION_KEY = 'agro_pending_operator_reg';
const DOCUMENTS_KEY = 'agro_operator_onboarding_docs';

function read(key) {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage unavailable (private mode / quota): the in-page state still works for this view
  }
}

function remove(key) {
  try {
    sessionStorage.removeItem(key);
  } catch {
    // ignore
  }
}

export function saveOnboardingSession({ onboardingToken, ...registration }) {
  write(REGISTRATION_KEY, registration);
  if (onboardingToken) {
    write(TOKEN_KEY, onboardingToken);
  } else {
    remove(TOKEN_KEY);
  }
  remove(DOCUMENTS_KEY);
}

export function getPendingRegistration() {
  return read(REGISTRATION_KEY);
}

export function updatePendingRegistration(changes) {
  const current = read(REGISTRATION_KEY) || {};
  const next = { ...current, ...changes };
  write(REGISTRATION_KEY, next);
  return next;
}

export function getOnboardingToken() {
  const token = read(TOKEN_KEY);
  return typeof token === 'string' && token ? token : null;
}

/** Ends the upload window on this device (the token itself expires on the server after 2 hours). */
export function clearOnboardingToken() {
  remove(TOKEN_KEY);
}

export function getUploadedDocuments() {
  return read(DOCUMENTS_KEY) || {};
}

export function saveUploadedDocuments(documents) {
  write(DOCUMENTS_KEY, documents);
}

export function clearOnboardingSession() {
  remove(TOKEN_KEY);
  remove(REGISTRATION_KEY);
  remove(DOCUMENTS_KEY);
}
