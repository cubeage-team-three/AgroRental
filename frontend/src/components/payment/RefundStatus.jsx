import { Link } from 'react-router-dom';
import { CheckCircle2, Clock, XCircle } from 'lucide-react';

/*
 * Farmer-facing refund status, from the refundStatus / refundAmount fields of the payment and invoice responses
 * (NONE, REFUND_PENDING, REFUND_FAILED, REFUNDED). The refund is separate from the payment: a refunded booking's
 * payment stays SUCCESS. Only REFUNDED means money has been sent back; pending and failed refunds are still owed.
 * The API does not expose the transfer reference or completion date to farmers, so neither is shown.
 */
const REFUND_META = {
  REFUND_PENDING: {
    label: 'Refund pending',
    badge: 'bg-amber-100 text-amber-800 border-amber-200',
    panel: 'border-amber-200 bg-amber-50 text-amber-900',
    icon: Clock,
    iconTone: 'text-amber-600',
  },
  REFUND_FAILED: {
    label: 'Refund transfer failed',
    badge: 'bg-red-100 text-red-800 border-red-200',
    panel: 'border-red-200 bg-red-50 text-red-900',
    icon: XCircle,
    iconTone: 'text-red-600',
  },
  REFUNDED: {
    label: 'Refunded',
    badge: 'bg-sky-100 text-sky-800 border-sky-200',
    panel: 'border-sky-200 bg-sky-50 text-sky-900',
    icon: CheckCircle2,
    iconTone: 'text-sky-600',
  },
};

function formatRupees(value) {
  if (value === null || value === undefined) return null;
  return `₹${Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Compact badge with the refund amount; renders nothing when there is no refund (NONE or missing). */
export function RefundBadge({ refundStatus, refundAmount }) {
  const meta = REFUND_META[refundStatus];
  if (!meta) return null;
  const Icon = meta.icon;
  const amount = formatRupees(refundAmount);
  return (
    <span className={`inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-[11px] font-extrabold ${meta.badge}`}>
      <Icon className="h-3 w-3" /> {meta.label}{amount ? ` · ${amount}` : ''}
    </span>
  );
}

/** Explanatory notice for a booking's refund; renders nothing when there is no refund (NONE or missing). */
export function RefundNotice({ refundStatus, refundAmount, bookingId, className = '' }) {
  const meta = REFUND_META[refundStatus];
  if (!meta) return null;
  const Icon = meta.icon;
  const amount = formatRupees(refundAmount) || 'the amount you paid';
  return (
    <div className={`flex items-start gap-3 rounded-xl border p-4 text-sm ${meta.panel} ${className}`}>
      <Icon className={`h-5 w-5 shrink-0 mt-0.5 ${meta.iconTone}`} />
      <div className="space-y-1">
        <p className="font-bold">{meta.label}</p>
        {refundStatus === 'REFUND_PENDING' && (
          <p>
            A full refund of {amount} has been initiated because this paid booking was cancelled or rejected.
            It has not been transferred to you yet. This page will show “Refunded” once AgroRent confirms the
            bank/UPI transfer.
          </p>
        )}
        {refundStatus === 'REFUND_FAILED' && (
          <p>
            The last attempt to transfer your refund of {amount} did not go through, so it has not reached you.
            The full amount is still owed to you and the transfer will be retried.
            {bookingId && (
              <>
                {' '}If your bank or UPI details have changed,{' '}
                <Link to={`/farmer/complaints?bookingId=${bookingId}`} className="font-bold underline">
                  raise a payment issue
                </Link>
                .
              </>
            )}
          </p>
        )}
        {refundStatus === 'REFUNDED' && (
          <p>
            A full refund of {amount} has been transferred to you. Check your bank or UPI statement for the credit.
          </p>
        )}
      </div>
    </div>
  );
}
