import { useState } from "react";
import { useNavigate } from "react-router-dom";

function RegisterOperator() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: "",
    mobileNumber: "",
    email: "",
    address: "",
    aadhaarNumber: "",
    drivingLicenseNumber: "",
    experience: "",
    skills: "",
    password: "",
    profilePhoto: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setLoading(true);
    setMessage("");
    setError("");

    try {
      const response = await fetch(
        "http://localhost:8080/api/operators/register",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            ...formData,
            experience: Number(formData.experience),
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data?.message || "Operator registration failed"
        );
      }

      const registeredMobile = formData.mobileNumber;
      setMessage("Operator registered successfully! Redirecting to OTP verification...");

      setTimeout(() => {
        navigate("/verify-otp", {
          state: { mobileNumber: registeredMobile },
        });
      }, 1200);
    } catch (err) {
      setError(err.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 py-10 px-4">
      <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-xl p-8">
        <h1 className="text-3xl font-bold text-green-700 mb-2">
          Operator Registration
        </h1>

        <p className="text-gray-600 mb-8">
          Register as an operator on the Agro Rental Platform.
        </p>

        {message && (
          <div className="mb-6 rounded-lg bg-green-100 border border-green-300 text-green-800 px-4 py-3">
            {message}
          </div>
        )}

        {error && (
          <div className="mb-6 rounded-lg bg-red-100 border border-red-300 text-red-800 px-4 py-3">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Full Name */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Full Name
            </label>

            <input
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="Enter your full name"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Mobile Number */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Mobile Number
            </label>

            <input
              type="tel"
              name="mobileNumber"
              value={formData.mobileNumber}
              onChange={handleChange}
              placeholder="Enter 10 digit mobile number"
              maxLength="10"
              pattern="[0-9]{10}"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Email */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Email
            </label>

            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Enter email address"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Address */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Address
            </label>

            <textarea
              name="address"
              value={formData.address}
              onChange={handleChange}
              placeholder="Enter your address"
              rows="3"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Aadhaar */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Aadhaar Number
            </label>

            <input
              type="text"
              name="aadhaarNumber"
              value={formData.aadhaarNumber}
              onChange={handleChange}
              placeholder="Enter 12 digit Aadhaar number"
              maxLength="12"
              pattern="[0-9]{12}"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Driving License */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Driving License Number
            </label>

            <input
              type="text"
              name="drivingLicenseNumber"
              value={formData.drivingLicenseNumber}
              onChange={handleChange}
              placeholder="Enter driving license number"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Experience */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Experience (Years)
            </label>

            <input
              type="number"
              name="experience"
              value={formData.experience}
              onChange={handleChange}
              placeholder="Enter experience in years"
              min="0"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Skills */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Skills
            </label>

            <input
              type="text"
              name="skills"
              value={formData.skills}
              onChange={handleChange}
              placeholder="Example: Tractor, Harvester"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Password */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Password
            </label>

            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Create a password"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Profile Photo */}
          <div>
            <label className="block font-semibold text-gray-700 mb-2">
              Profile Photo
            </label>

            <input
              type="text"
              name="profilePhoto"
              value={formData.profilePhoto}
              onChange={handleChange}
              placeholder="Example: operator.jpg"
              required
              className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-green-700 hover:bg-green-800 text-white font-bold py-3 px-6 rounded-lg transition disabled:bg-gray-400"
          >
            {loading ? "Registering..." : "Register Operator"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default RegisterOperator;