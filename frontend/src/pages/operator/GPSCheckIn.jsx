import { useState } from "react";
import { Link } from "react-router-dom";

function GPSCheckIn() {
  const [checkingIn, setCheckingIn] = useState(false);
  const [locationStatus, setLocationStatus] = useState("Ready for Check-In");
  const [coordinates, setCoordinates] = useState({ lat: 29.6857, lng: 76.9905 });
  const [lastUpdated, setLastUpdated] = useState(null);

  const handleCheckIn = () => {
    setCheckingIn(true);
    setLocationStatus("Acquiring GPS Satellite Fix...");

    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setCoordinates({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
          setLocationStatus("GPS Location Verified (Accuracy: ±5m)");
          setLastUpdated(new Date().toLocaleTimeString("en-IN"));
          setCheckingIn(false);
        },
        () => {
          // Fallback simulation
          setTimeout(() => {
            setCoordinates({ lat: 29.6912, lng: 76.9984 });
            setLocationStatus("Field Check-in Confirmed (Simulated GPS)");
            setLastUpdated(new Date().toLocaleTimeString("en-IN"));
            setCheckingIn(false);
          }, 1000);
        }
      );
    } else {
      setTimeout(() => {
        setLocationStatus("Field Check-in Confirmed (Network GPS)");
        setLastUpdated(new Date().toLocaleTimeString("en-IN"));
        setCheckingIn(false);
      }, 1000);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">📍</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 9 — Live Location & Geotagging
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Field GPS Check-In & Route Tracking
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Confirm your physical presence at designated farm coordinates and provide real-time field progress to dispatchers.
          </p>
        </div>

        <Link
          to="/operator/dashboard"
          className="bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-bold px-4 py-2.5 rounded-xl transition"
        >
          ← Dashboard
        </Link>
      </div>

      {/* Main GPS Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Map View Placeholder & Coordinates */}
        <div className="lg:col-span-2 bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-6">
          <div className="flex items-center justify-between border-b border-amber-100/60 pb-3">
            <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
              Live Field Map Coordinates
            </h2>
            <span className="text-xs font-bold text-emerald-800 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full">
              GPS Satellite Lock: Active
            </span>
          </div>

          {/* Interactive Map Visual Mock */}
          <div className="h-64 sm:h-80 w-full bg-gradient-to-br from-emerald-900 via-green-900 to-emerald-950 rounded-2xl relative overflow-hidden flex flex-col items-center justify-center p-6 text-white text-center shadow-inner">
            <div className="w-16 h-16 rounded-full bg-lime-400/20 border-2 border-lime-400 flex items-center justify-center text-3xl animate-pulse mb-3 shadow-lg">
              🚜
            </div>
            <p className="font-bold text-lg text-lime-300">
              Farm Field Sector 9 — Karnal Region
            </p>
            <p className="text-xs text-emerald-200 font-mono mt-1">
              Latitude: {coordinates.lat.toFixed(6)} | Longitude: {coordinates.lng.toFixed(6)}
            </p>
            {lastUpdated && (
              <span className="mt-3 px-3 py-1 bg-white/10 backdrop-blur-md rounded-full text-[11px] font-medium border border-white/20">
                Last Geotag Ping: {lastUpdated}
              </span>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4 text-xs">
            <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/60 space-y-1">
              <span className="text-gray-400 font-bold uppercase tracking-wider block">
                Target Farm Site
              </span>
              <p className="font-bold text-gray-800 text-sm">
                Plot 14B, Wheat Farm, Karnal
              </p>
              <p className="text-emerald-700 font-medium">Distance: At Location (0.0 km)</p>
            </div>

            <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/60 space-y-1">
              <span className="text-gray-400 font-bold uppercase tracking-wider block">
                Telemetry Status
              </span>
              <p className="font-bold text-emerald-800 text-sm">
                {locationStatus}
              </p>
              <p className="text-gray-500 font-medium">Precision Geofence: OK</p>
            </div>
          </div>
        </div>

        {/* Right Col: Check-In Action Card */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-6 border border-amber-100/70 shadow-sm space-y-5">
            <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
              <span className="text-xl">📍</span>
              <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                Geotag Check-In
              </h2>
            </div>

            <p className="text-xs text-gray-600 leading-relaxed">
              Press the button below when arriving at the job site to confirm your deployment start time and log equipment coordinates.
            </p>

            <button
              type="button"
              disabled={checkingIn}
              onClick={handleCheckIn}
              className="w-full bg-lime-400 hover:bg-lime-300 disabled:opacity-50 text-emerald-950 font-black text-sm py-4 rounded-2xl shadow-md transition-all flex items-center justify-center gap-2"
            >
              <span>{checkingIn ? "Acquiring Fix..." : "📍 Check-In at Job Location"}</span>
            </button>

            <div className="p-4 bg-emerald-50 rounded-2xl border border-emerald-200 text-xs text-emerald-950 space-y-1">
              <p className="font-bold">🔒 GPS Geofence Security</p>
              <p className="text-emerald-900/80 leading-relaxed">
                Check-in coordinates verify machinery operation within approved field boundaries.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default GPSCheckIn;
