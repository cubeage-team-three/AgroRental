import { Link } from "react-router-dom";
import OperatorBenefits from "../../components/operator/OperatorBenefits";
import OperatorHowItWorks from "../../components/operator/OperatorHowItWorks";
import OperatorRequirements from "../../components/operator/OperatorRequirements";
import operatorHeroImg from "../../assets/images/ModuleService Images/Operator.jpeg";

function OperatorLanding() {
  const bulletPoints = [
    "Simple registration with Aadhaar & license verification",
    "Get assigned to agricultural jobs",
    "Accept or reject jobs from your dashboard",
    "Live GPS check-in when a job starts",
    "Daily and job-based earnings tracking",
    "Job history and performance ratings",
    "Fast payouts after job completion",
    "Job alerts and notifications",
    "Multi-language support",
  ];

  return (
    <div className="bg-[#FDFBF7] min-h-screen">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-16 md:pt-12 md:pb-24">
        <div className="bg-white rounded-3xl md:rounded-[2.5rem] border border-amber-100/80 shadow-sm p-6 sm:p-10 lg:p-14 overflow-hidden relative">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-12 items-center">
            {/* Left Column (Content) */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-emerald-50 text-emerald-900 rounded-full text-xs font-black uppercase tracking-wider border border-emerald-200 shadow-sm">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span>🚜 Certified Machinery Drivers & Operators</span>
              </div>

              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-emerald-950 font-serif tracking-tight leading-tight">
                Get Matched to Jobs Near You
              </h1>

              <p className="text-gray-600 text-sm sm:text-base lg:text-lg leading-relaxed max-w-xl">
                Turn your agricultural driving skills into reliable earnings. Get verified, receive nearby machinery jobs, and manage your work from one place.
              </p>

              {/* Feature Bullets */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                {bulletPoints.map((bullet, idx) => (
                  <div key={idx} className="flex items-start gap-2.5 text-xs sm:text-sm text-gray-700 font-medium">
                    <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-800 font-bold text-xs flex items-center justify-center shrink-0 mt-0.5">
                      ✓
                    </span>
                    <span>{bullet}</span>
                  </div>
                ))}
              </div>

              {/* CTAs */}
              <div className="pt-4 flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5">
                <Link
                  to="/operator/register"
                  className="bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-sm px-8 py-4 rounded-2xl shadow-md hover:shadow-lg transition-all transform hover:-translate-y-0.5 text-center flex items-center justify-center gap-2"
                >
                  <span>Get Started as Operator</span>
                  <span>→</span>
                </Link>
                <Link
                  to="/login"
                  className="bg-emerald-50 hover:bg-emerald-100 text-emerald-900 font-bold text-sm px-6 py-4 rounded-2xl border border-emerald-200 transition text-center"
                >
                  Already registered? Login
                </Link>
              </div>
            </div>

            {/* Right Column (Hero Image & Stats Card) */}
            <div className="lg:col-span-5 relative">
              <div className="relative rounded-3xl overflow-hidden shadow-2xl border-4 border-white bg-gradient-to-tr from-emerald-900 to-green-800">
                <img
                  src={operatorHeroImg}
                  alt="Agricultural Machinery Operator"
                  className="w-full h-80 sm:h-96 lg:h-[460px] object-cover hover:scale-105 transition-transform duration-500"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>

                {/* Floating Verified Operators Badge */}
                <div className="absolute bottom-6 left-6 right-6 bg-white/95 backdrop-blur-md rounded-2xl p-4 shadow-xl border border-emerald-100 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-emerald-800 text-lime-300 flex items-center justify-center text-xl font-black shadow">
                      🚜
                    </div>
                    <div>
                      <p className="text-xl font-black text-emerald-950">12,000+</p>
                      <p className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                        Verified Operators
                      </p>
                    </div>
                  </div>
                  <span className="px-3 py-1 bg-emerald-100 text-emerald-900 text-xs font-bold rounded-full border border-emerald-200">
                    Active Pan-India
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <OperatorBenefits />

      {/* How It Works Section */}
      <OperatorHowItWorks />

      {/* Requirements Section */}
      <OperatorRequirements />
    </div>
  );
}

export default OperatorLanding;
