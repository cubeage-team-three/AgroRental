import { Link } from "react-router-dom";

function OperatorRequirements() {
  const requirements = [
    "Valid 10-digit mobile number for OTP authentication",
    "Government-issued Aadhaar Card for digital KYC verification",
    "Valid Driving License (Commercial or Heavy Agricultural Vehicle)",
    "Prior hands-on experience operating tractors, harvesters, or seeders",
    "Basic smartphone with Internet connectivity for assignment updates",
    "Bank account or UPI ID for direct payout settlements",
  ];

  return (
    <section className="py-16 md:py-24 bg-gradient-to-b from-white to-[#FDFBF7] border-t border-amber-100/60">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="bg-gradient-to-br from-emerald-900 to-green-950 text-white rounded-3xl p-8 md:p-12 shadow-xl relative overflow-hidden space-y-8">
          <div className="relative z-10 space-y-4 max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur-md rounded-full text-xs font-bold uppercase tracking-wider border border-white/20 text-lime-300">
              <span>📋</span>
              <span>Eligibility Checklist</span>
            </div>
            <h2 className="text-3xl md:text-4xl font-extrabold font-serif tracking-tight text-white">
              Ready to Become a Verified Operator?
            </h2>
            <p className="text-emerald-100/90 text-sm md:text-base leading-relaxed">
              Check off the simple requirements below and start receiving high-paying farm machinery deployments across your region.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 relative z-10">
            {requirements.map((req, idx) => (
              <div
                key={idx}
                className="flex items-start gap-3 bg-white/10 backdrop-blur-sm p-4 rounded-2xl border border-white/15 text-xs md:text-sm font-medium text-emerald-50"
              >
                <span className="w-5 h-5 rounded-full bg-lime-400 text-emerald-950 font-black text-xs flex items-center justify-center shrink-0 mt-0.5 shadow-sm">
                  ✓
                </span>
                <span>{req}</span>
              </div>
            ))}
          </div>

          <div className="pt-4 flex flex-col sm:flex-row items-center gap-4 relative z-10">
            <Link
              to="/operator/register"
              className="w-full sm:w-auto bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-sm px-8 py-4 rounded-2xl shadow-lg transition-all transform hover:-translate-y-0.5 text-center"
            >
              Start Operator Registration →
            </Link>
            <Link
              to="/login"
              className="w-full sm:w-auto bg-white/10 hover:bg-white/20 text-white font-bold text-sm px-6 py-4 rounded-2xl border border-white/20 backdrop-blur-sm transition text-center"
            >
              Already Registered? Log In
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

export default OperatorRequirements;
