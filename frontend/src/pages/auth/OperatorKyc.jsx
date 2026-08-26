import { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  ArrowRight,
  Award,
  CheckCircle2,
  FileCheck,
  FileText,
  HardHat,
  Image,
  Loader2,
  ShieldCheck,
  Upload,
  User,
  X,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';

const REQUIRED_DOCS = [
  {
    type: 'AADHAAR',
    label: 'Aadhaar Card (Front/Back or PDF)',
    description: 'Government identity proof (12-digit Aadhaar matching registration)',
    icon: ShieldCheck,
    required: true,
  },
  {
    type: 'DRIVING_LICENSE',
    label: 'Driving License (LMV / Transport / Tractor)',
    description: 'Commercial / tractor driving permit to operate heavy farm equipment',
    icon: Award,
    required: true,
  },
  {
    type: 'PROFILE_PHOTO',
    label: 'Operator Profile Photo / Avatar',
    description: 'Clear front-facing passport photograph for farmer recognition',
    icon: User,
    required: false,
  },
  {
    type: 'EXPERIENCE_CERTIFICATE',
    label: 'Experience Certificate (Optional)',
    description: 'Prior machinery operation proof or recommendation letter from fleet owner',
    icon: FileText,
    required: false,
  },
];

function OperatorKyc() {
  const location = useLocation();
  const navigate = useNavigate();

  // Retrieve registration context
  const [regData, setRegData] = useState(() => {
    if (location.state?.operatorId) {
      return location.state;
    }
    const saved = sessionStorage.getItem('agro_pending_operator_reg');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return null;
      }
    }
    return null;
  });

  const operatorId = regData?.operatorId || null;
  const fullName = regData?.fullName || 'Operator';
  const aadhaarNumber = regData?.aadhaarNumber || '';
  const dlNumber = regData?.drivingLicenseNumber || '';

  const [documentsState, setDocumentsState] = useState({});
  const [uploadingType, setUploadingType] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Load existing uploaded documents on mount
  useEffect(() => {
    if (operatorId) {
      loadExistingDocuments(operatorId);
    }
  }, [operatorId]);

  const loadExistingDocuments = async (id) => {
    try {
      const res = await operatorService.getOperatorDocuments(id);
      const docs = res?.data || res || [];
      if (Array.isArray(docs) && docs.length > 0) {
        const mapped = {};
        docs.forEach((doc) => {
          mapped[doc.documentType] = {
            id: doc.id,
            fileName: doc.fileName,
            fileSize: doc.fileSize,
            fileUrl: doc.fileUrl,
            status: doc.verificationStatus || 'PENDING',
          };
        });
        setDocumentsState((prev) => ({ ...prev, ...mapped }));
      }
    } catch (err) {
      console.warn('Could not fetch existing documents:', err.message);
    }
  };

  const handleFileSelect = async (docType, e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!operatorId) {
      setErrorMessage('Operator identity session expired. Please restart registration.');
      return;
    }

    setErrorMessage('');
    setUploadingType(docType);

    try {
      // Simulate/create secure document payload
      const mockStorageUrl = `https://storage.agrorent.in/kyc/operator_${operatorId}_${docType.toLowerCase()}_${Date.now()}.${file.name.split('.').pop()}`;

      const payload = {
        documentType: docType,
        documentNumber:
          docType === 'AADHAAR'
            ? aadhaarNumber
            : docType === 'DRIVING_LICENSE'
              ? dlNumber
              : null,
        fileName: file.name,
        fileUrl: mockStorageUrl,
        fileSize: file.size,
        mimeType: file.type || 'application/octet-stream',
      };

      const res = await operatorService.uploadOperatorDocument(operatorId, payload);
      const savedDoc = res?.data || res;

      setDocumentsState((prev) => ({
        ...prev,
        [docType]: {
          id: savedDoc.id,
          fileName: file.name,
          fileSize: file.size,
          fileUrl: mockStorageUrl,
          status: savedDoc.verificationStatus || 'PENDING',
        },
      }));

      setSuccessMessage(`✓ Uploaded ${docType.replace('_', ' ')} successfully.`);
    } catch (err) {
      console.error('Document Upload Error:', err);
      setErrorMessage(err.message || `Failed to upload ${docType}. Please try again.`);
    } finally {
      setUploadingType(null);
    }
  };

  const handleRemoveDoc = (docType) => {
    setDocumentsState((prev) => {
      const copy = { ...prev };
      delete copy[docType];
      return copy;
    });
  };

  const handleCompleteKyc = async () => {
    setErrorMessage('');

    // Check mandatory documents (Aadhaar & DL)
    const hasAadhaar = documentsState.AADHAAR;
    const hasDL = documentsState.DRIVING_LICENSE;

    if (!hasAadhaar || !hasDL) {
      setErrorMessage('Please upload both your Aadhaar Card and Driving License to proceed.');
      return;
    }

    setSubmitting(true);

    try {
      // Update session storage
      const finalRegSession = {
        ...regData,
        documentsSubmitted: true,
      };
      sessionStorage.setItem('agro_pending_operator_reg', JSON.stringify(finalRegSession));

      setTimeout(() => {
        navigate('/register/operator/pending', {
          state: finalRegSession,
        });
      }, 800);
    } catch (err) {
      setErrorMessage(err.message || 'Error finalizing document submission.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <div className="flex items-center gap-2 text-emerald-800 text-xs font-bold uppercase tracking-wider mb-2">
          <FileCheck className="h-4 w-4 text-emerald-600" />
          Step 3 of 4 • KYC & License Verification
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Submit Verification Documents
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Upload clear scanned copies or photos of your government credentials for administrative approval.
        </p>
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
          <div className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorMessage('')}
              className="shrink-0 text-red-400 hover:text-red-600 transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </RevealItem>
      )}

      {successMessage && (
        <RevealItem className="mt-5">
          <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
        </RevealItem>
      )}

      {/* Document Upload Cards */}
      <RevealItem className="mt-6 space-y-3.5">
        {REQUIRED_DOCS.map((doc) => {
          const isUploaded = !!documentsState[doc.type];
          const isUploading = uploadingType === doc.type;
          const currentDoc = documentsState[doc.type];

          return (
            <div
              key={doc.type}
              className={`rounded-2xl border p-4 transition-all duration-200 ${
                isUploaded
                  ? 'border-emerald-300 bg-emerald-50/40'
                  : 'border-slate-200/80 bg-[#F7F6F0] hover:border-emerald-200'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                      isUploaded ? 'bg-emerald-700 text-white' : 'bg-slate-200 text-slate-600'
                    }`}
                  >
                    <doc.icon className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-slate-900">{doc.label}</h4>
                      {doc.required ? (
                        <span className="rounded-md bg-amber-100 px-1.5 py-0.5 text-[10px] font-bold text-amber-800">
                          Required
                        </span>
                      ) : (
                        <span className="rounded-md bg-slate-200 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">
                          Optional
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">{doc.description}</p>
                    {isUploaded && (
                      <div className="mt-2 flex items-center gap-2 text-xs font-semibold text-emerald-800">
                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                        <span>{currentDoc.fileName}</span>
                        <span className="text-[10px] text-slate-400">
                          ({(currentDoc.fileSize / 1024).toFixed(1)} KB)
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="shrink-0">
                  {isUploaded ? (
                    <button
                      type="button"
                      onClick={() => handleRemoveDoc(doc.type)}
                      className="rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition-colors"
                    >
                      Change
                    </button>
                  ) : (
                    <label className="cursor-pointer inline-flex items-center gap-1.5 rounded-xl bg-emerald-800 px-3.5 py-2 text-xs font-bold text-white shadow-sm hover:bg-emerald-900 active:scale-95 transition-all">
                      {isUploading ? (
                        <>
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          Uploading...
                        </>
                      ) : (
                        <>
                          <Upload className="h-3.5 w-3.5" />
                          Upload
                        </>
                      )}
                      <input
                        type="file"
                        accept="image/*,.pdf"
                        disabled={isUploading}
                        onChange={(e) => handleFileSelect(doc.type, e)}
                        className="hidden"
                      />
                    </label>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </RevealItem>

      <RevealItem className="mt-8">
        <MagneticButton className="block w-full">
          <motion.button
            type="button"
            disabled={submitting}
            onClick={handleCompleteKyc}
            animate={
              submitting
                ? {}
                : {
                    boxShadow: [
                      '0 0 20px 0px rgba(163,230,53,0.35)',
                      '0 0 38px 6px rgba(163,230,53,0.6)',
                      '0 0 20px 0px rgba(163,230,53,0.35)',
                    ],
                  }
            }
            transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
            className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-60"
          >
            {submitting ? (
              <>
                <Loader2 className="h-[18px] w-[18px] animate-spin" />
                Submitting Documents for Verification...
              </>
            ) : (
              <>
                Submit Documents & Finish Registration
                <ArrowRight className="h-4 w-4" />
              </>
            )}
          </motion.button>
        </MagneticButton>
      </RevealItem>

      <RevealItem className="mt-6 text-center text-xs text-slate-400">
        All documents are transmitted securely via encrypted storage and used strictly for identity & license authorization.
      </RevealItem>
    </RevealGroup>
  );
}

export default OperatorKyc;
