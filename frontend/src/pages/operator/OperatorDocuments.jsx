import { useState, useEffect } from "react";
import { useLocation, Link } from "react-router-dom";

function OperatorDocuments() {
  const location = useLocation();
  const initialMobile =
    location.state?.mobileNumber ||
    new URLSearchParams(location.search).get("mobileNumber") ||
    "";

  const [mobileNumber, setMobileNumber] = useState(initialMobile);
  const [operatorSummary, setOperatorSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploadingType, setUploadingType] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // Selected files for upload
  const [selectedFiles, setSelectedFiles] = useState({
    AADHAAR_CARD: null,
    DRIVING_LICENSE: null,
    EXPERIENCE_CERTIFICATE: null,
  });

  const [docNumbers, setDocNumbers] = useState({
    AADHAAR_CARD: "",
    DRIVING_LICENSE: "",
    EXPERIENCE_CERTIFICATE: "",
  });

  useEffect(() => {
    if (initialMobile) {
      fetchDocuments(initialMobile);
    }
  }, [initialMobile]);

  const fetchDocuments = async (mobile) => {
    const targetMobile = mobile || mobileNumber;
    if (!targetMobile) return;

    setLoading(true);
    setError("");

    try {
      const response = await fetch(
        `http://localhost:8080/api/operators/documents?mobileNumber=${targetMobile}`
      );
      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "Failed to fetch document status");
      }

      setOperatorSummary(result.data);
    } catch (err) {
      setError(err.message || "Could not connect to server");
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (docType, file) => {
    if (!file) return;

    // Validate size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError("File size exceeds 10MB limit");
      return;
    }

    setSelectedFiles((prev) => ({
      ...prev,
      [docType]: file,
    }));
    setError("");
  };

  const handleUpload = async (docType) => {
    const file = selectedFiles[docType];
    if (!file) {
      setError("Please select a file to upload");
      return;
    }

    setUploadingType(docType);
    setError("");
    setMessage("");

    try {
      const formData = new FormData();
      formData.append("mobileNumber", mobileNumber);
      formData.append("documentType", docType);
      if (docNumbers[docType]) {
        formData.append("documentNumber", docNumbers[docType]);
      }
      formData.append("file", file);

      const response = await fetch(
        "http://localhost:8080/api/operators/documents/upload",
        {
          method: "POST",
          body: formData,
        }
      );

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "Upload failed");
      }

      setMessage(`${formatDocTypeName(docType)} uploaded successfully!`);
      setSelectedFiles((prev) => ({ ...prev, [docType]: null }));
      await fetchDocuments(mobileNumber);
    } catch (err) {
      setError(err.message || "Failed to upload document");
    } finally {
      setUploadingType(null);
    }
  };

  // Admin verification simulation for testing
  const handleAdminVerify = async (docId, status, docTypeName) => {
    let rejectionReason = "";
    if (status === "REJECTED") {
      rejectionReason =
        window.prompt(
          `Enter rejection reason for ${docTypeName}:`,
          "Blurry image / Expired document"
        ) || "Document details could not be verified";
    }

    setLoading(true);
    try {
      const response = await fetch(
        `http://localhost:8080/api/operators/documents/${docId}/verify`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            status,
            rejectionReason: status === "REJECTED" ? rejectionReason : null,
            verifiedBy: "Compliance Admin",
          }),
        }
      );

      const result = await response.json();
      if (!response.ok || !result.success) {
        throw new Error(result.message || "Verification action failed");
      }

      setMessage(`Document marked as ${status}`);
      await fetchDocuments(mobileNumber);
    } catch (err) {
      setError(err.message || "Action failed");
    } finally {
      setLoading(false);
    }
  };

  const getDocFromSummary = (docType) => {
    return operatorSummary?.documents?.find(
      (d) => d.documentType === docType
    );
  };

  const formatDocTypeName = (type) => {
    switch (type) {
      case "AADHAAR_CARD":
        return "Aadhaar Card";
      case "DRIVING_LICENSE":
        return "Driving License";
      case "EXPERIENCE_CERTIFICATE":
        return "Experience / Skill Certificate";
      default:
        return type;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "VERIFIED":
        return (
          <span className="px-2.5 py-1 bg-green-100 text-green-800 text-xs font-bold rounded-full">
            ✓ Verified
          </span>
        );
      case "REJECTED":
        return (
          <span className="px-2.5 py-1 bg-red-100 text-red-800 text-xs font-bold rounded-full">
            ✕ Rejected
          </span>
        );
      case "PENDING":
        return (
          <span className="px-2.5 py-1 bg-yellow-100 text-yellow-800 text-xs font-bold rounded-full">
            ⏳ Under Review
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 bg-gray-100 text-gray-700 text-xs font-semibold rounded-full">
            Not Submitted
          </span>
        );
    }
  };

  const renderDocCard = (docType, isMandatory, icon) => {
    const existingDoc = getDocFromSummary(docType);
    const selectedFile = selectedFiles[docType];
    const isUploading = uploadingType === docType;

    return (
      <div className="bg-white rounded-2xl p-6 border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <span className="text-3xl">{icon}</span>
            <div>
              <h3 className="font-bold text-gray-800 flex items-center gap-2">
                {formatDocTypeName(docType)}
                {isMandatory && (
                  <span className="text-xs text-red-600 font-semibold">
                    *Mandatory
                  </span>
                )}
              </h3>
              <p className="text-xs text-gray-500">
                PDF, JPG, PNG, or WEBP (Max 10MB)
              </p>
            </div>
          </div>
          <div>{getStatusBadge(existingDoc?.verificationStatus)}</div>
        </div>

        {/* Existing uploaded file details */}
        {existingDoc && (
          <div className="mb-4 p-3 bg-gray-50 rounded-xl text-xs space-y-1.5 border border-gray-100">
            <div className="flex justify-between">
              <span className="text-gray-500">Uploaded File:</span>
              <a
                href={`http://localhost:8080${existingDoc.fileDownloadUrl}`}
                target="_blank"
                rel="noreferrer"
                className="text-green-700 hover:text-green-900 font-bold underline truncate max-w-[200px]"
              >
                {existingDoc.fileName}
              </a>
            </div>
            {existingDoc.documentNumber && (
              <div className="flex justify-between">
                <span className="text-gray-500">Document No:</span>
                <span className="font-semibold text-gray-800">
                  {existingDoc.documentNumber}
                </span>
              </div>
            )}
            {existingDoc.rejectionReason && (
              <div className="mt-2 p-2 bg-red-50 text-red-700 rounded-lg font-medium">
                ⚠️ Rejection Note: {existingDoc.rejectionReason}
              </div>
            )}

            {/* Admin Verification Controls for testing */}
            <div className="pt-2 mt-2 border-t border-gray-200 flex gap-2">
              <button
                type="button"
                onClick={() =>
                  handleAdminVerify(existingDoc.id, "VERIFIED", formatDocTypeName(docType))
                }
                className="text-[11px] bg-green-600 hover:bg-green-700 text-white font-semibold px-3 py-1 rounded-lg transition"
              >
                ✓ Admin Verify
              </button>
              <button
                type="button"
                onClick={() =>
                  handleAdminVerify(existingDoc.id, "REJECTED", formatDocTypeName(docType))
                }
                className="text-[11px] bg-red-600 hover:bg-red-700 text-white font-semibold px-3 py-1 rounded-lg transition"
              >
                ✕ Admin Reject
              </button>
            </div>
          </div>
        )}

        {/* File input / Upload area */}
        <div className="space-y-3">
          <input
            type="file"
            id={`file-${docType}`}
            accept=".pdf,image/png,image/jpeg,image/webp"
            onChange={(e) => handleFileSelect(docType, e.target.files[0])}
            className="hidden"
          />

          <div className="flex gap-2">
            <label
              htmlFor={`file-${docType}`}
              className="flex-1 text-center bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold py-2.5 px-4 rounded-xl cursor-pointer border border-dashed border-gray-300 transition"
            >
              {selectedFile
                ? `Selected: ${selectedFile.name}`
                : existingDoc
                ? "Choose New File to Replace"
                : "Choose File to Upload"}
            </label>

            {selectedFile && (
              <button
                type="button"
                onClick={() => handleUpload(docType)}
                disabled={isUploading}
                className="bg-green-700 hover:bg-green-800 text-white font-bold text-xs px-4 py-2.5 rounded-xl transition disabled:bg-gray-400"
              >
                {isUploading ? "Uploading..." : "Upload Now"}
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  const aadhaarDoc = getDocFromSummary("AADHAAR_CARD");
  const dlDoc = getDocFromSummary("DRIVING_LICENSE");
  const bothVerified =
    aadhaarDoc?.verificationStatus === "VERIFIED" &&
    dlDoc?.verificationStatus === "VERIFIED";

  return (
    <div className="min-h-screen bg-gray-100 py-10 px-4">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header Banner */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-200">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <span className="text-xs font-bold text-green-700 uppercase tracking-wider">
                Module 3 — Document Verification
              </span>
              <h1 className="text-2xl font-bold text-gray-800 mt-1">
                Operator Document Submission
              </h1>
              <p className="text-sm text-gray-500 mt-1">
                Upload your identity and license documents for administrative compliance and verification.
              </p>
            </div>

            {/* Operator Quick Status */}
            {operatorSummary && (
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 text-right">
                <p className="text-xs text-gray-500">Operator</p>
                <p className="text-base font-bold text-gray-800">
                  {operatorSummary.fullName}
                </p>
                <p className="text-xs font-medium text-gray-600">
                  +91 {operatorSummary.mobileNumber}
                </p>
                <div className="mt-2 flex gap-1.5 justify-end">
                  <span className="px-2 py-0.5 bg-green-100 text-green-800 text-[10px] font-bold rounded">
                    Mobile OTP Verified ✓
                  </span>
                  <span
                    className={`px-2 py-0.5 text-[10px] font-bold rounded ${
                      operatorSummary.operatorStatus === "APPROVED"
                        ? "bg-green-100 text-green-800"
                        : "bg-yellow-100 text-yellow-800"
                    }`}
                  >
                    Status: {operatorSummary.operatorStatus}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Search/Fetch Bar if mobile not set */}
          {!initialMobile && (
            <div className="mt-4 pt-4 border-t border-gray-100 flex gap-2 max-w-md">
              <input
                type="tel"
                value={mobileNumber}
                onChange={(e) => setMobileNumber(e.target.value)}
                placeholder="Enter 10-digit registered mobile"
                maxLength="10"
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
              />
              <button
                type="button"
                onClick={() => fetchDocuments(mobileNumber)}
                disabled={loading}
                className="bg-green-700 hover:bg-green-800 text-white font-semibold text-xs px-4 py-2 rounded-lg transition"
              >
                {loading ? "Loading..." : "Load Status"}
              </button>
            </div>
          )}
        </div>

        {/* Message Banner */}
        {message && (
          <div className="p-4 rounded-xl bg-green-50 border border-green-200 text-green-800 text-sm flex items-start gap-3">
            <span className="text-green-600 font-bold">✓</span>
            <div>{message}</div>
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-start gap-3">
            <span className="text-red-600 font-bold">⚠️</span>
            <div>{error}</div>
          </div>
        )}

        {/* Both Verified Celebratory Card */}
        {bothVerified && (
          <div className="bg-green-700 text-white rounded-2xl p-6 shadow-lg text-center space-y-3">
            <div className="text-4xl">🎉</div>
            <h2 className="text-xl font-bold">All Documents Verified & Approved!</h2>
            <p className="text-green-100 text-sm max-w-md mx-auto">
              Your documents have been verified by compliance admin. Your operator account is fully approved for logging in and accepting jobs.
            </p>
            <div className="pt-2">
              <Link
                to="/login"
                className="inline-block bg-white text-green-800 hover:bg-green-50 font-bold py-3 px-8 rounded-xl shadow-md transition"
              >
                Proceed to Operator Login (Module 4) →
              </Link>
            </div>
          </div>
        )}

        {/* Document Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {renderDocCard("AADHAAR_CARD", true, "🪪")}
          {renderDocCard("DRIVING_LICENSE", true, "🚗")}
          {renderDocCard("EXPERIENCE_CERTIFICATE", false, "📜")}
        </div>

        {/* Back Link */}
        <div className="text-center pt-4">
          <Link
            to="/verify-otp"
            state={{ mobileNumber }}
            className="text-xs text-gray-500 hover:text-green-700 transition"
          >
            ← Back to OTP Verification
          </Link>
        </div>
      </div>
    </div>
  );
}

export default OperatorDocuments;
