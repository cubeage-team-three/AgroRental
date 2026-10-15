import { useState, useEffect, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import { adminService } from '../../services/adminService';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  ExternalLink,
  History,
  IndianRupee,
  Info,
  Lock,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react';

const STATUS_TABS = [
  { id: 'ALL', label: 'All' },
  { id: 'REFUND_PENDING', label: 'Pending' },
  { id: 'REFUND_FAILED', label: 'Failed' },
  { id: 'REFUNDED', label: 'Refunded' },
];

// Wording never implies money has moved unless the refund is REFUNDED (verified transfer recorded)
const STATUS_META = {
  REFUND_PENDING: {
    label: 'Pending Transfer',
    hint: 'Owed — no transfer recorded yet',
    style: 'bg-amber-100 text-amber-800 border-amber-300',
    icon: Clock,
  },
  REFUND_FAILED: {
    label: 'Transfer Failed',
    hint: 'Still owed — retry required',
    style: 'bg-red-100 text-red-800 border-red-300',
    icon: XCircle,
  },
  REFUNDED: {
    label: 'Refunded',
    hint: 'Verified transfer recorded',
    style: 'bg-emerald-100 text-emerald-800 border-emerald-300',
    icon: CheckCircle2,
  },
};

// Mirrors the backend (RefundService / Refund*Request DTOs)
const TRANSFER_REFERENCE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._/-]{5,63}$/;
const MAX_REFERENCE = 64;
const MAX_TEXT = 500;

function formatAmount(value) {
  if (value === null || value === undefined) return '—';
  return `₹${Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function sumAmounts(refunds) {
  // Sum in paise to avoid floating-point drift
  const paise = refunds.reduce((acc, r) => acc + Math.round(Number(r.amount || 0) * 100), 0);
  return paise / 100;
}

function formatDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/** Actor names recorded by the backend: FARMER_<id>, PARTNER_<id>, ADMIN_<id>, ADMIN_USER_<id>. */
function formatActor(actor) {
  if (!actor) return '—';
  const match = /^(FARMER|PARTNER|ADMIN_USER|ADMIN)_(\d+)$/.exec(actor);
  if (!match) return actor;
  const labels = { FARMER: 'Farmer', PARTNER: 'Partner', ADMIN: 'Admin', ADMIN_USER: 'Admin user' };
  return `${labels[match[1]]} #${match[2]}`;
}

function statusLabel(status) {
  return STATUS_META[status]?.label || status || '—';
}

/** Turns an apiClient error into a safe, useful message (field validation, 401/403/404, server message). */
function describeError(err, fallback) {
  if (!err) return fallback;
  if (err.status === 401) return 'Your admin session has expired. Please sign in again.';
  if (err.status === 403) return 'Access denied. Only administrators can view or manage refunds.';
  const fieldErrors = err.data?.data && typeof err.data.data === 'object' && !Array.isArray(err.data.data)
    ? Object.values(err.data.data).filter((v) => typeof v === 'string')
    : [];
  if (fieldErrors.length > 0) return fieldErrors.join(' ');
  return err.message || fallback;
}

function RefundStatusBadge({ status }) {
  const meta = STATUS_META[status];
  const Icon = meta?.icon;
  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-black uppercase tracking-wide border shadow-2xs ${
        meta?.style || 'bg-gray-100 text-gray-600 border-gray-300'
      }`}
    >
      {Icon && <Icon className="w-3.5 h-3.5" />}
      <span>{statusLabel(status)}</span>
    </span>
  );
}

function DetailRow({ label, children }) {
  return (
    <div className="flex justify-between gap-4 py-1.5">
      <span className="text-slate-500 shrink-0">{label}</span>
      <span className="font-bold text-slate-800 text-right break-words">{children}</span>
    </div>
  );
}

function validateAction(mode, form) {
  if (mode === 'complete') {
    const reference = form.transferReference.trim();
    if (!reference) return 'Enter the verified bank/UPI transfer reference.';
    if (reference.length > MAX_REFERENCE) return `The transfer reference cannot exceed ${MAX_REFERENCE} characters.`;
    if (!TRANSFER_REFERENCE_PATTERN.test(reference)) {
      return 'The transfer reference must be 6–64 characters of letters, digits, ".", "_", "/" or "-", start with a letter or digit, and contain no spaces.';
    }
    if (form.note.trim().length > MAX_TEXT) return `The note cannot exceed ${MAX_TEXT} characters.`;
  }
  if (mode === 'fail') {
    if (!form.reason.trim()) return 'A failure reason is required.';
    if (form.reason.trim().length > MAX_TEXT) return `The reason cannot exceed ${MAX_TEXT} characters.`;
  }
  if (mode === 'retry' && form.note.trim().length > MAX_TEXT) {
    return `The note cannot exceed ${MAX_TEXT} characters.`;
  }
  return null;
}

const EMPTY_FORM = { transferReference: '', note: '', reason: '' };

function RefundDetailModal({ refundId, onClose, onChanged }) {
  const [refund, setRefund] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [booking, setBooking] = useState(null);
  const [bookingError, setBookingError] = useState(null);

  const [mode, setMode] = useState(null); // 'complete' | 'fail' | 'retry'
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [verifiedAck, setVerifiedAck] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState(null);
  // Synchronous in-flight guard: state updates are not visible to a second click in the same tick
  const submittingRef = useRef(false);

  const loadRefund = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await adminService.getRefundById(refundId);
      setRefund(data);
      return data;
    } catch (err) {
      setRefund(null);
      setLoadError(err.status === 404
        ? `Refund #${refundId} was not found.`
        : describeError(err, `Failed to load refund #${refundId}.`));
      return null;
    } finally {
      setLoading(false);
    }
  }, [refundId]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const data = await loadRefund();
      if (cancelled || !data?.bookingId) return;
      try {
        const bookingData = await adminService.getBookingById(data.bookingId);
        if (!cancelled) setBooking(bookingData);
      } catch (err) {
        if (!cancelled) setBookingError(describeError(err, 'Booking details could not be loaded.'));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadRefund]);

  const resetAction = () => {
    setMode(null);
    setForm(EMPTY_FORM);
    setFormError(null);
    setConfirming(false);
    setVerifiedAck(false);
  };

  const startAction = (nextMode) => {
    resetAction();
    setActionError(null);
    setMode(nextMode);
  };

  const handleReview = (e) => {
    e.preventDefault();
    const problem = validateAction(mode, form);
    setFormError(problem);
    if (!problem) {
      // Every confirmation needs a fresh acknowledgement (e.g. after correcting a rejected reference)
      setVerifiedAck(false);
      setConfirming(true);
    }
  };

  const handleConfirm = async () => {
    if (submittingRef.current || !refund) return;
    submittingRef.current = true;
    setSubmitting(true);
    setActionError(null);
    const id = refund.id;
    try {
      if (mode === 'complete') {
        await adminService.completeRefund(id, {
          transferReference: form.transferReference.trim(),
          note: form.note.trim() || null,
        });
      } else if (mode === 'fail') {
        await adminService.failRefund(id, { reason: form.reason.trim() });
      } else if (mode === 'retry') {
        await adminService.retryRefund(id, { note: form.note.trim() || null });
      }
      resetAction();
      // Show only what the server now reports, never an optimistic status
      const fresh = await loadRefund();
      onChanged(fresh
        ? `Refund #${id} is now ${statusLabel(fresh.status)} (confirmed by the server).`
        : `Refund #${id} was updated. Reload the list to see its current status.`);
    } catch (err) {
      setActionError(describeError(err, 'The refund could not be updated.'));
      setConfirming(false);
      // The refund may have changed concurrently; show its current server state
      await loadRefund();
      onChanged(null);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  const status = refund?.status;
  const amount = formatAmount(refund?.amount);
  const nextAttempt = (refund?.attemptCount || 1) + 1;

  return (
    <div className="fixed inset-0 z-50 flex items-start sm:items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4 overflow-y-auto" role="dialog" aria-modal="true" aria-labelledby="refund-detail-title">
      <div className="w-full max-w-3xl rounded-3xl bg-white p-6 sm:p-8 shadow-2xl border border-slate-200 my-8">
        <div className="flex items-start justify-between border-b border-slate-100 pb-4">
          <div>
            <h3 id="refund-detail-title" className="text-lg font-black text-slate-900">Refund #{refundId}</h3>
            {refund && (
              <p className="mt-0.5 text-xs text-slate-500 font-medium">
                Payment #{refund.paymentId} · Booking #{refund.bookingId} · {formatActor(`FARMER_${refund.farmerId}`)}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            aria-label="Close"
            className="text-slate-400 hover:text-slate-700 p-1 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {loading && !refund ? (
          <div className="py-12 text-center">
            <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
            <p className="text-xs text-slate-500 font-medium">Loading refund record…</p>
          </div>
        ) : loadError && !refund ? (
          <div className="mt-5 p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
            <span>{loadError}</span>
          </div>
        ) : refund ? (
          <div className="mt-5 space-y-5 text-xs">
            {/* Status summary */}
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <RefundStatusBadge status={status} />
                <span className="text-slate-500 font-medium">{STATUS_META[status]?.hint}</span>
              </div>
              <div className="text-right">
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Refund amount (full payment)</p>
                <p className="text-2xl font-black text-slate-900">{amount}</p>
              </div>
            </div>

            {status === 'REFUND_PENDING' && (
              <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 font-medium flex gap-2">
                <Info className="w-4 h-4 shrink-0 text-amber-600 mt-0.5" />
                <span>
                  No transfer has been recorded for this refund. Mark it complete only after the money has reached the
                  farmer and you have the verified bank/UPI reference.
                </span>
              </div>
            )}
            {status === 'REFUND_FAILED' && (
              <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-900 font-medium flex gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                <span>The last transfer attempt failed. The full amount is still owed to the farmer.</span>
              </div>
            )}

            {loadError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-2xl text-red-700 font-bold">{loadError}</div>
            )}

            <div className="grid gap-4 md:grid-cols-2">
              {/* Refund record */}
              <div className="rounded-2xl border border-slate-200 p-4">
                <h4 className="text-[10px] font-extrabold uppercase tracking-wider text-slate-400 mb-2">Refund record</h4>
                <DetailRow label="Initiated">{formatDateTime(refund.initiatedAt)}</DetailRow>
                <DetailRow label="Initiated by">{formatActor(refund.initiatedBy)}</DetailRow>
                <DetailRow label="Reason">{refund.initiationReason || '—'}</DetailRow>
                <DetailRow label="Transfer attempts">{refund.attemptCount ?? '—'}</DetailRow>
                {refund.lastFailureReason && (
                  <>
                    <DetailRow label="Last failure">{refund.lastFailureReason}</DetailRow>
                    <DetailRow label="Failure recorded">
                      {formatDateTime(refund.lastFailedAt)} · {formatActor(refund.lastFailedBy)}
                    </DetailRow>
                  </>
                )}
                {status === 'REFUNDED' && (
                  <>
                    <DetailRow label="Transfer reference">{refund.transferReference || '—'}</DetailRow>
                    <DetailRow label="Completed">
                      {formatDateTime(refund.completedAt)} · {formatActor(refund.completedBy)}
                    </DetailRow>
                  </>
                )}
              </div>

              {/* Booking */}
              <div className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-[10px] font-extrabold uppercase tracking-wider text-slate-400">Booking #{refund.bookingId}</h4>
                  <Link
                    to={`/admin/bookings?bookingId=${refund.bookingId}`}
                    className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 hover:text-emerald-900"
                  >
                    Open in Bookings <ExternalLink className="w-3 h-3" />
                  </Link>
                </div>
                {booking ? (
                  <>
                    <DetailRow label="Equipment">{booking.equipmentName || '—'}</DetailRow>
                    <DetailRow label="Farmer">{booking.farmerName || formatActor(`FARMER_${refund.farmerId}`)}</DetailRow>
                    <DetailRow label="Partner">{booking.partnerBusinessName || booking.partnerName || '—'}</DetailRow>
                    <DetailRow label="Dates">{booking.startDate || '—'} to {booking.endDate || '—'}</DetailRow>
                    <DetailRow label="Booking status">{booking.status || '—'}</DetailRow>
                    <DetailRow label="Booking total">{formatAmount(booking.totalCost)}</DetailRow>
                  </>
                ) : (
                  <p className="text-slate-500 font-medium py-1.5">{bookingError || 'Loading booking…'}</p>
                )}
              </div>
            </div>

            {/* Audit history */}
            <div className="rounded-2xl border border-slate-200 p-4">
              <h4 className="text-[10px] font-extrabold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5" /> Audit history
              </h4>
              {Array.isArray(refund.history) && refund.history.length > 0 ? (
                <ol className="space-y-3 border-l-2 border-slate-200 pl-4">
                  {refund.history.map((event, idx) => (
                    <li key={`${event.occurredAt}-${idx}`} className="relative">
                      <span className="absolute -left-[1.4rem] top-1 h-2.5 w-2.5 rounded-full bg-emerald-600 ring-2 ring-white" />
                      <p className="font-bold text-slate-800">
                        {event.fromStatus ? `${statusLabel(event.fromStatus)} → ` : 'Created as '}
                        {statusLabel(event.toStatus)}
                      </p>
                      <p className="text-[11px] text-slate-500">
                        {formatDateTime(event.occurredAt)} · {formatActor(event.actor)}
                      </p>
                      {event.transferReference && (
                        <p className="text-[11px] text-slate-700">Reference: <span className="font-mono">{event.transferReference}</span></p>
                      )}
                      {event.note && <p className="text-[11px] text-slate-600 italic">“{event.note}”</p>}
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="text-slate-500 font-medium">No audit events were returned for this refund.</p>
              )}
            </div>

            {actionError && (
              <div className="p-3.5 bg-red-50 border border-red-200 rounded-2xl text-red-700 font-bold flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
                <span>{actionError}</span>
              </div>
            )}

            {/* Actions */}
            {status === 'REFUNDED' ? (
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-slate-600 font-medium flex items-center gap-2">
                <Lock className="w-4 h-4 text-slate-500 shrink-0" />
                <span>This refund is complete. Completed refunds cannot be changed.</span>
              </div>
            ) : !mode ? (
              <div className="flex flex-wrap justify-end gap-3 pt-3 border-t border-slate-100">
                {status === 'REFUND_PENDING' && (
                  <>
                    <button
                      type="button"
                      onClick={() => startAction('fail')}
                      className="rounded-xl border border-red-200 bg-white px-4 py-2 text-xs font-bold text-red-700 hover:bg-red-50 transition-colors cursor-pointer"
                    >
                      Mark Transfer Failed
                    </button>
                    <button
                      type="button"
                      onClick={() => startAction('complete')}
                      className="rounded-xl bg-emerald-700 px-5 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-800 transition-colors cursor-pointer"
                    >
                      Record Verified Transfer
                    </button>
                  </>
                )}
                {status === 'REFUND_FAILED' && (
                  <button
                    type="button"
                    onClick={() => startAction('retry')}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-800 transition-colors cursor-pointer"
                  >
                    <RotateCcw className="w-3.5 h-3.5" /> Retry Transfer
                  </button>
                )}
              </div>
            ) : !confirming ? (
              <form onSubmit={handleReview} className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4 space-y-3" noValidate>
                <h4 className="text-sm font-black text-slate-900">
                  {mode === 'complete' && 'Record verified transfer'}
                  {mode === 'fail' && 'Record failed transfer'}
                  {mode === 'retry' && 'Retry transfer'}
                </h4>

                {mode === 'complete' && (
                  <div>
                    <label htmlFor="refund-reference" className="mb-1.5 block text-[11px] font-bold uppercase tracking-wider text-slate-500">
                      Bank / UPI transfer reference (UTR) *
                    </label>
                    <input
                      id="refund-reference"
                      type="text"
                      autoComplete="off"
                      maxLength={MAX_REFERENCE + 10}
                      value={form.transferReference}
                      onChange={(e) => setForm({ ...form, transferReference: e.target.value })}
                      placeholder="e.g. UTR from the bank statement"
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                    />
                    <p className="mt-1 text-[11px] text-slate-500">
                      6–64 letters, digits, ".", "_", "/" or "-", no spaces. Each reference can be used for only one refund.
                    </p>
                  </div>
                )}

                {mode === 'fail' && (
                  <div>
                    <label htmlFor="refund-reason" className="mb-1.5 block text-[11px] font-bold uppercase tracking-wider text-slate-500">
                      Failure reason *
                    </label>
                    <textarea
                      id="refund-reason"
                      rows={3}
                      value={form.reason}
                      onChange={(e) => setForm({ ...form, reason: e.target.value })}
                      placeholder="e.g. Beneficiary account closed, UPI handle inactive…"
                      className="w-full rounded-xl border border-slate-200 bg-white p-3 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                    />
                    <p className="mt-1 text-[11px] text-slate-500">{form.reason.trim().length}/{MAX_TEXT}</p>
                  </div>
                )}

                {(mode === 'complete' || mode === 'retry') && (
                  <div>
                    <label htmlFor="refund-note" className="mb-1.5 block text-[11px] font-bold uppercase tracking-wider text-slate-500">
                      Note (optional, recorded in the audit history)
                    </label>
                    <textarea
                      id="refund-note"
                      rows={2}
                      value={form.note}
                      onChange={(e) => setForm({ ...form, note: e.target.value })}
                      className="w-full rounded-xl border border-slate-200 bg-white p-3 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                    />
                    <p className="mt-1 text-[11px] text-slate-500">{form.note.trim().length}/{MAX_TEXT}</p>
                  </div>
                )}

                {formError && <p className="text-xs font-bold text-red-700">{formError}</p>}

                <div className="flex justify-end gap-3 pt-1">
                  <button
                    type="button"
                    onClick={resetAction}
                    className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="rounded-xl bg-emerald-700 px-5 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-800 transition-colors cursor-pointer"
                  >
                    Review & Confirm
                  </button>
                </div>
              </form>
            ) : (
              <div className="rounded-2xl border-2 border-amber-300 bg-amber-50 p-4 space-y-3">
                <h4 className="text-sm font-black text-slate-900 flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-amber-600" />
                  {mode === 'complete' && 'Confirm verified refund transfer'}
                  {mode === 'fail' && 'Confirm failed transfer'}
                  {mode === 'retry' && 'Confirm retry'}
                </h4>

                {mode === 'complete' && (
                  <>
                    <p className="text-slate-800 font-medium leading-relaxed">
                      You are recording that <strong>{amount}</strong> has been transferred to{' '}
                      <strong>{booking?.farmerName || formatActor(`FARMER_${refund.farmerId}`)}</strong> for Booking #{refund.bookingId},
                      with transfer reference <strong className="font-mono">{form.transferReference.trim()}</strong>.
                    </p>
                    <p className="text-red-800 font-bold">
                      The refund will be marked REFUNDED. This cannot be undone: a completed refund cannot be failed,
                      retried or edited, and the reference cannot be used again.
                    </p>
                    <label className="flex items-start gap-2 font-bold text-slate-800 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={verifiedAck}
                        onChange={(e) => setVerifiedAck(e.target.checked)}
                        className="mt-0.5 h-4 w-4 accent-emerald-700"
                      />
                      <span>I have verified this transfer and its reference in the bank/UPI statement.</span>
                    </label>
                  </>
                )}
                {mode === 'fail' && (
                  <p className="text-slate-800 font-medium leading-relaxed">
                    The refund of <strong>{amount}</strong> will be marked <strong>Transfer Failed</strong> with the reason
                    “{form.reason.trim()}”. No money is recorded as returned: the full amount stays owed to the farmer and
                    the refund can be retried.
                  </p>
                )}
                {mode === 'retry' && (
                  <p className="text-slate-800 font-medium leading-relaxed">
                    The refund of <strong>{amount}</strong> moves back to <strong>Pending Transfer</strong> as attempt{' '}
                    {nextAttempt}, on the same refund record. This does not transfer any money; record the completed
                    transfer only after the new transfer has succeeded.
                  </p>
                )}

                <div className="flex justify-end gap-3 pt-1">
                  <button
                    type="button"
                    onClick={() => setConfirming(false)}
                    disabled={submitting}
                    className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={handleConfirm}
                    disabled={submitting || (mode === 'complete' && !verifiedAck)}
                    className={`rounded-xl px-5 py-2 text-xs font-bold text-white shadow-xs transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
                      mode === 'fail' ? 'bg-red-600 hover:bg-red-700' : 'bg-emerald-700 hover:bg-emerald-800'
                    }`}
                  >
                    {submitting
                      ? 'Submitting…'
                      : mode === 'complete'
                        ? `Confirm ${amount} Refunded`
                        : mode === 'fail'
                          ? 'Confirm Transfer Failed'
                          : 'Confirm Retry'}
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function ManagePayments() {
  const [activeTab, setActiveTab] = useState('ALL');
  const [refunds, setRefunds] = useState([]);
  const [allRefunds, setAllRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successToast, setSuccessToast] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRefundId, setSelectedRefundId] = useState(null);
  const activeRequestId = useRef(0);

  const fetchRefunds = useCallback(async () => {
    const requestId = ++activeRequestId.current;
    setLoading(true);
    setError(null);
    try {
      // The status filter is applied by the server; the unfiltered list supplies the tab counts and totals
      const [tabData, allData] = activeTab === 'ALL'
        ? await adminService.getRefunds().then((all) => [all, all])
        : await Promise.all([adminService.getRefunds(activeTab), adminService.getRefunds()]);
      if (requestId !== activeRequestId.current) return;
      setRefunds(Array.isArray(tabData) ? tabData : []);
      setAllRefunds(Array.isArray(allData) ? allData : []);
    } catch (err) {
      if (requestId !== activeRequestId.current) return;
      console.error('Failed to load refunds for admin:', err);
      setRefunds([]);
      setAllRefunds([]);
      setError(describeError(err, 'Failed to load refunds.'));
    } finally {
      if (requestId === activeRequestId.current) setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchRefunds();
  }, [fetchRefunds]);

  const handleRefundChanged = (message) => {
    if (message) setSuccessToast(`✓ ${message}`);
    fetchRefunds();
  };

  const countFor = (status) => allRefunds.filter((r) => r.status === status).length;
  const totalFor = (status) => sumAmounts(allRefunds.filter((r) => r.status === status));

  const query = searchQuery.toLowerCase().trim();
  const filteredRefunds = refunds.filter((r) => {
    if (!query) return true;
    return [
      r.id, r.bookingId, r.paymentId, r.farmerId, r.partnerId,
      r.transferReference, r.initiatedBy, r.initiationReason, r.lastFailureReason,
    ].some((value) => value !== null && value !== undefined && String(value).toLowerCase().includes(query));
  });

  const summaryCards = [
    { status: 'REFUND_PENDING', title: 'Owed — pending transfer', tone: 'text-amber-700' },
    { status: 'REFUND_FAILED', title: 'Owed — transfer failed', tone: 'text-red-700' },
    { status: 'REFUNDED', title: 'Refunded — verified', tone: 'text-emerald-700' },
  ];

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 space-y-6 font-sans">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Refund Management
            </h1>
            <span className="px-2.5 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-full text-[10px] font-extrabold uppercase">
              Admin Console
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 font-medium mt-1">
            Full refunds owed for paid bookings that were cancelled or rejected. Record a refund as complete only after
            verifying the bank/UPI transfer.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchRefunds}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-xl transition shadow-xs shrink-0 cursor-pointer self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh List</span>
        </button>
      </div>

      {/* Success Toast */}
      {successToast && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-900 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button type="button" onClick={() => setSuccessToast('')} className="text-emerald-700 hover:text-emerald-900 font-bold ml-2 cursor-pointer">✕</button>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchRefunds} className="text-xs font-bold text-red-800 underline cursor-pointer">
            Retry
          </button>
        </div>
      )}

      {/* Summary (from the server's unfiltered refund list) */}
      {!error && (
        <div className="grid gap-4 sm:grid-cols-3">
          {summaryCards.map((card) => (
            <div key={card.status} className="bg-white rounded-3xl border border-slate-200 shadow-xs p-4 sm:p-5">
              <div className="flex items-center justify-between">
                <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500">{card.title}</p>
                <IndianRupee className={`h-4 w-4 ${card.tone}`} />
              </div>
              <p className={`mt-2 text-2xl font-extrabold ${card.tone}`}>
                {loading ? '—' : formatAmount(totalFor(card.status))}
              </p>
              <p className="mt-1 text-[11px] text-slate-400 font-medium">
                {loading ? '' : `${countFor(card.status)} refund${countFor(card.status) === 1 ? '' : 's'}`}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Filters & Search */}
      <div className="bg-white rounded-3xl p-4 sm:p-6 border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search refund, booking, payment, farmer ID or reference..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto overflow-x-auto text-xs">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-xl font-bold transition-all shrink-0 cursor-pointer ${
                activeTab === tab.id
                  ? 'bg-emerald-700 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {tab.label}
              {!loading && !error && ` (${tab.id === 'ALL' ? allRefunds.length : countFor(tab.id)})`}
            </button>
          ))}
        </div>
      </div>

      {/* Refunds Table */}
      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((idx) => (
            <div key={idx} className="h-16 bg-slate-100 rounded-2xl border border-slate-200" />
          ))}
        </div>
      ) : error ? null : filteredRefunds.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-3xl p-12 text-center space-y-3 shadow-xs">
          <ShieldCheck className="w-12 h-12 text-slate-300 mx-auto" />
          <h3 className="text-sm font-bold text-slate-800">No Refunds Found</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            {searchQuery
              ? 'No refunds match the search.'
              : activeTab !== 'ALL'
                ? `There are no refunds with status ${statusLabel(activeTab)}.`
                : 'No refunds have been initiated yet.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-[10px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-100">
                <tr>
                  <th className="px-6 py-4">Refund</th>
                  <th className="px-6 py-4">Booking</th>
                  <th className="px-6 py-4 text-right">Amount</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Initiated</th>
                  <th className="px-6 py-4">Latest outcome</th>
                  <th className="px-6 py-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredRefunds.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80 transition-colors align-top">
                    <td className="px-6 py-4 text-xs font-mono font-bold text-slate-400">
                      #{r.id}
                      <div className="mt-0.5 text-[10px] font-normal text-slate-400">Payment #{r.paymentId}</div>
                    </td>

                    <td className="px-6 py-4">
                      <Link
                        to={`/admin/bookings?bookingId=${r.bookingId}`}
                        className="inline-flex items-center gap-1 font-extrabold text-emerald-700 hover:text-emerald-900"
                      >
                        Booking #{r.bookingId} <ExternalLink className="w-3 h-3" />
                      </Link>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {formatActor(`FARMER_${r.farmerId}`)}
                        {r.partnerId !== null && r.partnerId !== undefined && ` · ${formatActor(`PARTNER_${r.partnerId}`)}`}
                      </div>
                    </td>

                    <td className="px-6 py-4 text-right font-extrabold text-slate-900">{formatAmount(r.amount)}</td>

                    <td className="px-6 py-4">
                      <RefundStatusBadge status={r.status} />
                      {r.attemptCount > 1 && (
                        <div className="mt-1 text-[10px] text-slate-400">Attempt {r.attemptCount}</div>
                      )}
                    </td>

                    <td className="px-6 py-4 text-[11px] text-slate-500">
                      <div>{formatDateTime(r.initiatedAt)}</div>
                      <div className="text-slate-400">by {formatActor(r.initiatedBy)}</div>
                    </td>

                    <td className="px-6 py-4 text-[11px] text-slate-500 max-w-xs">
                      {r.status === 'REFUNDED' ? (
                        <>
                          <div>Ref <span className="font-mono text-slate-700">{r.transferReference || '—'}</span></div>
                          <div className="text-slate-400">{formatDateTime(r.completedAt)}</div>
                        </>
                      ) : r.lastFailureReason ? (
                        <>
                          <p className="line-clamp-2 text-red-700">Failed: {r.lastFailureReason}</p>
                          <div className="text-slate-400">{formatDateTime(r.lastFailedAt)}</div>
                        </>
                      ) : (
                        <span className="text-slate-400">No transfer recorded yet</span>
                      )}
                    </td>

                    <td className="px-6 py-4 text-right">
                      <button
                        type="button"
                        onClick={() => setSelectedRefundId(r.id)}
                        className="px-3.5 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition-colors cursor-pointer"
                      >
                        {r.status === 'REFUNDED' ? 'View' : 'Review'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selectedRefundId !== null && (
        <RefundDetailModal
          key={selectedRefundId}
          refundId={selectedRefundId}
          onClose={() => setSelectedRefundId(null)}
          onChanged={handleRefundChanged}
        />
      )}
    </div>
  );
}

export default ManagePayments;
