import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  MapPin,
  Clock,
  Phone,
  Navigation,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft,
  RefreshCw,
  Tractor,
  UserCheck,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import { trackingService } from '../../services/trackingService';
import { bookingService } from '../../services/bookingService';

function LiveTracking() {
  const { id } = useParams();
  const [tracking, setTracking] = useState(null);
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchTracking = async (isManual = false) => {
    if (isManual) setRefreshing(true);
    try {
      if (id) {
        const [trackingData, bookingData] = await Promise.all([
          trackingService.getLiveTracking(id),
          bookingService.getBookingById(id).catch(() => null)
        ]);
        setTracking(trackingData);
        if (bookingData) setBooking(bookingData);
      }
    } catch (err) {
      console.error('Failed to load live tracking:', err);
      setError('Unable to load real-time GPS tracking data.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchTracking();
    const interval = setInterval(() => fetchTracking(), 10000);
    return () => clearInterval(interval);
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-6 space-y-6 font-sans animate-pulse">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="h-96 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  const progressPercent = tracking?.workProgress ?? 50;

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div className="flex items-center gap-3">
          <Link
            to="/farmer/my-bookings"
            className="p-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
                Live Machine Tracking
              </h1>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-black bg-emerald-100 text-emerald-800 border border-emerald-300">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
                LIVE GPS
              </span>
            </div>
            <p className="text-xs text-gray-500 font-medium">
              Booking Reservation #{id || '101'}
            </p>
          </div>
        </div>

        <button
          onClick={() => fetchTracking(true)}
          disabled={refreshing}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 hover:border-emerald-500 text-gray-700 text-xs font-bold rounded-xl shadow-sm transition disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin text-emerald-600' : ''}`} />
          <span>Refresh GPS</span>
        </button>
      </div>

      {error && (
        <div className="p-4 bg-amber-50 border-l-4 border-amber-500 rounded-2xl text-amber-800 text-xs font-bold flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 2 Cols: Live Map & Status Bar */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Visual Map Simulator Card */}
          <div className="bg-slate-900 rounded-3xl p-6 relative overflow-hidden text-white shadow-xl min-h-[320px] flex flex-col justify-between border border-slate-800">
            {/* Grid Pattern overlay */}
            <div 
              className="absolute inset-0 opacity-20 pointer-events-none"
              style={{
                backgroundImage: 'radial-gradient(#10b981 1px, transparent 1px)',
                backgroundSize: '24px 24px'
              }}
            />

            {/* Map Top Badge Bar */}
            <div className="relative z-10 flex items-center justify-between">
              <div className="bg-slate-800/80 backdrop-blur-md px-3.5 py-1.5 rounded-full border border-slate-700 flex items-center gap-2">
                <MapPin className="w-4 h-4 text-emerald-400 animate-bounce" />
                <span className="text-xs font-bold text-slate-200">
                  Lat: {tracking?.latitude?.toFixed(4) || '18.5204'}, Lng: {tracking?.longitude?.toFixed(4) || '73.8567'}
                </span>
              </div>

              <div className="bg-emerald-500/20 backdrop-blur-md text-emerald-300 border border-emerald-500/40 px-3.5 py-1.5 rounded-full text-xs font-black flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-emerald-400" />
                <span>ETA: {tracking?.eta || '20 mins'}</span>
              </div>
            </div>

            {/* Map Simulation Graphical Representation */}
            <div className="relative z-10 my-8 flex flex-col items-center justify-center space-y-4">
              <div className="relative">
                <div className="w-20 h-20 rounded-full bg-emerald-500/10 border-2 border-emerald-500/40 animate-pulse flex items-center justify-center">
                  <div className="w-12 h-12 rounded-full bg-emerald-500/30 flex items-center justify-center">
                    <Tractor className="w-7 h-7 text-emerald-400" />
                  </div>
                </div>
                <div className="absolute -top-1 -right-1 bg-emerald-500 text-slate-950 font-black text-[10px] px-1.5 py-0.5 rounded-full shadow-lg">
                  ACTIVE
                </div>
              </div>

              <div className="text-center space-y-1">
                <h3 className="text-base font-black text-white">
                  {tracking?.equipmentName || booking?.equipmentName || 'Mahindra Tractor 575 DI'}
                </h3>
                <p className="text-xs text-slate-400 font-medium max-w-sm">
                  {tracking?.routeInformation || 'En route to registered farm field'}
                </p>
              </div>
            </div>

            {/* Map Footer Bar */}
            <div className="relative z-10 bg-slate-800/90 backdrop-blur-md rounded-2xl p-3 border border-slate-700 flex items-center justify-between text-xs text-slate-300">
              <span className="flex items-center gap-1.5 font-bold">
                <Zap className="w-4 h-4 text-amber-400" /> Live Field Telemetry
              </span>
              <span className="text-[11px] text-slate-400 font-mono">
                Updated: {tracking?.lastUpdated ? new Date(tracking.lastUpdated).toLocaleTimeString() : 'Just now'}
              </span>
            </div>
          </div>

          {/* Progress Timeline Card */}
          <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-black text-gray-900 uppercase tracking-wider">
                Work Completion Progress
              </h3>
              <span className="text-xs font-black text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                {progressPercent}% Completed
              </span>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden p-0.5 border border-gray-200">
              <div
                className="bg-gradient-to-r from-emerald-500 to-teal-600 h-full rounded-full transition-all duration-700 ease-out"
                style={{ width: `${progressPercent}%` }}
              />
            </div>

            {/* Status Milestones */}
            <div className="grid grid-cols-4 gap-2 pt-2 text-center text-[11px] font-bold text-gray-500">
              <div className="space-y-1">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 mx-auto" />
                <span className="text-gray-900">Dispatched</span>
              </div>
              <div className="space-y-1">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 mx-auto" />
                <span className="text-gray-900">On the Way</span>
              </div>
              <div className="space-y-1">
                <CheckCircle2 className={`w-4 h-4 ${progressPercent >= 30 ? 'text-emerald-600' : 'text-gray-300'} mx-auto`} />
                <span className={progressPercent >= 30 ? 'text-gray-900' : ''}>Arrived at Farm</span>
              </div>
              <div className="space-y-1">
                <CheckCircle2 className={`w-4 h-4 ${progressPercent >= 100 ? 'text-emerald-600' : 'text-gray-300'} mx-auto`} />
                <span className={progressPercent >= 100 ? 'text-gray-900' : ''}>Job Completed</span>
              </div>
            </div>
          </div>

        </div>

        {/* Right 1 Col: Operator Info & Booking Summary */}
        <div className="space-y-6">
          
          {/* Operator Details Card */}
          <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-xs font-black text-gray-500 uppercase tracking-wider">
              <UserCheck className="w-4 h-4 text-emerald-600" /> Assigned Operator
            </div>

            <div className="flex items-center gap-3 pt-1">
              <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-800 font-black text-lg flex items-center justify-center shrink-0 border border-emerald-200">
                {tracking?.operatorName ? tracking.operatorName.charAt(0) : 'O'}
              </div>
              <div>
                <h4 className="text-sm font-black text-gray-900">
                  {tracking?.operatorName || 'Verified Machinery Operator'}
                </h4>
                <p className="text-xs text-gray-500 flex items-center gap-1 font-medium">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" /> AgroRental Certified
                </p>
              </div>
            </div>

            <a
              href={`tel:${tracking?.operatorMobile || ''}`}
              className="w-full py-2.5 px-4 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-sm transition flex items-center justify-center gap-2"
            >
              <Phone className="w-4 h-4" /> Call Operator Now
            </a>
          </div>

          {/* Service Details Summary Card */}
          <div className="bg-emerald-950 text-white rounded-3xl p-6 space-y-3 shadow-sm border border-emerald-900">
            <h4 className="text-xs font-black text-emerald-400 uppercase tracking-wider">
              Booking Overview
            </h4>

            <div className="space-y-2 text-xs font-medium divide-y divide-emerald-900/60">
              <div className="flex justify-between py-1 text-emerald-100">
                <span className="text-emerald-400">Equipment</span>
                <span className="font-bold">{tracking?.equipmentName || booking?.equipmentName || 'Agricultural Machinery'}</span>
              </div>
              <div className="flex justify-between py-1 text-emerald-100">
                <span className="text-emerald-400">Status</span>
                <span className="font-black text-emerald-300">{tracking?.status || 'WORK_STARTED'}</span>
              </div>
              <div className="flex justify-between py-1 text-emerald-100">
                <span className="text-emerald-400">Destination</span>
                <span className="font-bold truncate max-w-[150px]">{booking?.deliveryAddress || 'Primary Farm'}</span>
              </div>
            </div>

            <div className="pt-2">
              <Link
                to={`/farmer/bookings/${id}`}
                className="w-full py-2 bg-emerald-900 hover:bg-emerald-800 text-emerald-200 text-xs font-bold rounded-xl transition text-center block"
              >
                View Full Booking Details
              </Link>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
}

export default LiveTracking;
