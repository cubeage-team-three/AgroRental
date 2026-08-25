const http = require('http');

async function request(options, body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let parsed;
        try { parsed = JSON.parse(data); } catch (e) { parsed = data; }
        resolve({ status: res.statusCode, data: parsed, raw: data });
      });
    });
    req.on('error', reject);
    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

async function runAudit() {
  console.log('=== CHECK 1: REGISTRATION & VALIDATION ===');
  const uniqueMobile = '98' + Math.floor(10000000 + Math.random() * 90000000);
  const regPayload = {
    fullName: 'Raju Shinde',
    mobileNumber: uniqueMobile,
    email: `raju_${uniqueMobile}@agrorent.in`,
    address: 'Wagholi, Pune, Maharashtra 412207',
    aadhaarNumber: '987654321098',
    drivingLicenseNumber: 'MH122023000999',
    experience: 4,
    skills: 'Tractor Operation, Rotavator',
    password: 'Password@123',
    profilePhoto: null
  };

  const regRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/register',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, regPayload);

  console.log(`Registration HTTP Status: ${regRes.status}`);
  console.log(`Operator ID: ${regRes.data?.data?.id}, Status: ${regRes.data?.data?.status}, mobileVerified: ${regRes.data?.data?.mobileVerified}`);
  const operatorId = regRes.data?.data?.id;

  console.log('\n=== CHECK 2: OTP GENERATION, EXPIRY, RESEND, VERIFY ===');
  // Send OTP
  const sendOtpRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/otp/send',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: uniqueMobile, purpose: 'MOBILE_VERIFICATION' });

  console.log(`OTP Send HTTP Status: ${sendOtpRes.status}`);
  console.log(`OTP Send Response Message: ${sendOtpRes.data?.data?.message}`);
  const devMockOtp = sendOtpRes.data?.data?.devMockOtp;
  console.log(`Dev Mock OTP received: ${devMockOtp ? 'YES (Valid 6-digit)' : 'NO'}`);

  // Test Resend Cooldown
  const resendCooldownRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/otp/send',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: uniqueMobile, purpose: 'MOBILE_VERIFICATION' });
  console.log(`Resend Cooldown Enforcement HTTP Status: ${resendCooldownRes.status} (Expected 400 with cooldown message: "${resendCooldownRes.data?.message}")`);

  // Test Wrong OTP
  const wrongOtpRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/otp/verify',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: uniqueMobile, otp: '000000', purpose: 'MOBILE_VERIFICATION' });
  console.log(`Wrong OTP Verify HTTP Status: ${wrongOtpRes.status} (Message: "${wrongOtpRes.data?.message}")`);

  // Test Correct OTP
  const correctOtpRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/otp/verify',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: uniqueMobile, otp: devMockOtp, purpose: 'MOBILE_VERIFICATION' });
  console.log(`Correct OTP Verify HTTP Status: ${correctOtpRes.status} (verified: ${correctOtpRes.data?.data?.verified}, Message: "${correctOtpRes.data?.data?.message}")`);

  // Test Replay / Already Used OTP
  const replayOtpRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/otp/verify',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: uniqueMobile, otp: devMockOtp, purpose: 'MOBILE_VERIFICATION' });
  console.log(`Replay/Already-Used OTP Verify HTTP Status: ${replayOtpRes.status} (Expected 400: "${replayOtpRes.data?.message}")`);

  console.log('\n=== CHECK 3: KYC / DOCUMENT SUBMISSION & VERIFICATION ===');
  // Upload Aadhaar Document
  const docPayload = {
    documentType: 'AADHAAR',
    documentNumber: '987654321098',
    fileName: 'aadhaar_card_raju.pdf',
    fileUrl: 'https://storage.agrorent.in/kyc/raju_aadhaar.pdf',
    fileSize: 1048576,
    mimeType: 'application/pdf'
  };

  const docRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/operators/${operatorId}/documents`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, docPayload);

  console.log(`Document Upload HTTP Status: ${docRes.status}`);
  console.log(`Uploaded Document ID: ${docRes.data?.data?.id}, Verification Status: ${docRes.data?.data?.verificationStatus}`);

  // Fetch Documents
  const getDocsRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/operators/${operatorId}/documents`,
    method: 'GET'
  });
  console.log(`Get Documents HTTP Status: ${getDocsRes.status}, Documents Count: ${getDocsRes.data?.data?.length}`);

  console.log('\n=== CHECK 4: DATA MASKING & SECURITY INSPECTION ===');
  // Seeded Operator Profile Check
  const seededLoginRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { mobileNumber: '9876543220', password: 'Operator@123' });

  const token = seededLoginRes.data?.data?.accessToken;
  const profileRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/operators/profile',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });

  console.log(`Profile Masked Aadhaar: "${profileRes.data?.data?.maskedAadhaarNumber}" (Raw Aadhaar exposed: ${profileRes.data?.data?.aadhaarNumber !== undefined})`);
  console.log(`Profile Masked DL:      "${profileRes.data?.data?.maskedDrivingLicenseNumber}" (Raw DL exposed: ${profileRes.data?.data?.drivingLicenseNumber !== undefined})`);
  console.log(`Password in Profile:    ${profileRes.data?.data?.password !== undefined ? 'EXPOSED' : 'PROTECTED (Not returned)'}`);
}

runAudit().catch(console.error);
