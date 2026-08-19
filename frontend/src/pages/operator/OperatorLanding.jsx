import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Wheat, Users, Tractor, BarChart3, CheckCircle2, ArrowRight } from "lucide-react";
import OperatorBenefits from "../../components/operator/OperatorBenefits";
import OperatorHowItWorks from "../../components/operator/OperatorHowItWorks";
import OperatorRequirements from "../../components/operator/OperatorRequirements";
import operatorHeroImg from "../../assets/images/ModuleService Images/Operator.jpeg";

function OperatorLanding() {
  const navigate = useNavigate();
  const [selectedRole, setSelectedRole] = useState("operator");

  const roles = [
    { id: "farmer", label: "Farmer", icon: Wheat, route: "/register/farmer" },
    { id: "operator", label: "Operator", icon: Users, route: "/operator" },
    { id: "equipment-owner", label: "Equipment Owner", icon: Tractor, route: "/register/partner" },
    { id: "admin", label: "Admin", icon: BarChart3, route: "/login" },
  ];

  const featurePoints = [
    "OTP signup with skill category tagging",
    "License and document upload with admin approval",
    "Online/offline toggle with daily acre capacity",
    "Nearby job alerts with one-tap accept/reject",
    "Status controls: On the way → Work started → Completed",
    "Acre counter, work hour timer, field photo upload",
    "Daily earnings summary and withdrawal requests",
  ];

  const handleRoleClick = (role) => {
    setSelectedRole(role.id);
    if (role.id !== "operator") {
      navigate(role.route);
    }
  };

  return (
    <div className="bg-[#FDFBF7] min-h-screen text-stone-800">
      {/* Main Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-16 md:pt-12 md:pb-20">
        
        {/* Top Role Selector */}
        <div className="flex justify-center mb-10 sm:mb-14">
          <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3 p-1.5 bg-transparent">
            {roles.map((role) => {
              const isSelected = selectedRole === role.id;
              const Icon = role.icon;
              return (
                <button
                  key={role.id}
                  onClick={() => handleRoleClick(role)}
                  type="button"
                  className={`inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 rounded-full text-sm font-medium transition-all duration-200 ${
                    isSelected
                      ? "bg-[#1d3d26] text-white shadow-md hover:bg-[#16331e]"
                      : "bg-white text-stone-700 border border-stone-200 hover:bg-stone-50 hover:border-stone-300 shadow-sm"
                  }`}
                >
                  <Icon className={`w-4 h-4 ${isSelected ? "text-white" : "text-stone-600"}`} />
                  <span>{role.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Hero Section */}
        <div className="max-w-6xl mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-14 items-center">
            
            {/* Left Column: Content */}
            <div className="lg:col-span-7 flex flex-col justify-center">
              <h1 className="text-3xl sm:text-4xl lg:text-[2.65rem] font-bold text-[#142e1c] font-serif leading-[1.2] tracking-tight mb-8">
                Earn More with Flexible Job Alerts
              </h1>

              {/* 7 Feature Points */}
              <div className="space-y-4 mb-9">
                {featurePoints.map((point, index) => (
                  <div key={index} className="flex items-center gap-3.5">
                    <CheckCircle2 className="w-5 h-5 text-[#c2621a] shrink-0 stroke-[2.2]" />
                    <span className="text-[15px] sm:text-base text-stone-700 font-normal leading-snug">
                      {point}
                    </span>
                  </div>
                ))}
              </div>

              {/* CTA Button */}
              <div>
                <Link
                  to="/operator/register"
                  className="inline-flex items-center justify-center gap-2.5 bg-[#b85d19] hover:bg-[#a35013] text-white text-base font-semibold px-7 sm:px-8 py-3.5 rounded-full shadow-md hover:shadow-lg transition-all duration-200 transform hover:-translate-y-0.5 cursor-pointer"
                >
                  <span>Get Started as Operator</span>
                  <ArrowRight className="w-5 h-5 stroke-[2.5]" />
                </Link>
              </div>
            </div>

            {/* Right Column: Agricultural Image */}
            <div className="lg:col-span-5 flex items-center justify-center">
              <div className="w-full relative overflow-hidden rounded-[22px] shadow-lg border border-stone-200/60 bg-stone-100">
                <img
                  src={operatorHeroImg}
                  alt="Agricultural Machinery Operator in Field"
                  className="w-full aspect-[16/11] sm:aspect-[16/10] lg:aspect-[4/3] object-cover block"
                />
              </div>
            </div>

          </div>
        </div>

      </div>

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
