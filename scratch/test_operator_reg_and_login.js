const http = require('http');

async function post(url, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const postData = JSON.stringify(body);
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname + u.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData),
        ...headers
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, data }));
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

async function get(url, headers = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname + u.search,
      method: 'GET',
      headers
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, data }));
    });
    req.on('error', reject);
    req.end();
  });
}

async function run() {
  console.log('=== TEST 1: OPERATOR REGISTRATION WITH NEW VALID OPERATOR ===');
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const newMobile = `98234${randomSuffix}0`;
  const newEmail = `operator_${randomSuffix}@agrorent.com`;

  const newOperatorPayload = {
    fullName: 'Anil Kumar More',
    mobileNumber: newMobile,
    email: newEmail,
    address: 'Near Gram Panchayat, Khed, Pune, Maharashtra 410501',
    aadhaarNumber: `45678901${randomSuffix}`,
    drivingLicenseNumber: `MH14${randomSuffix}2022`,
    experience: 5,
    skills: 'Tractor Operation, Combine Harvester, Rotavator & Cultivator',
    password: 'Password@123',
    profilePhoto: 'https://images.unsplash.com/photo-1544717305-2782549b5136'
  };

  const regRes = await post('http://localhost:8080/api/operators/register', newOperatorPayload);
  console.log(`Registration HTTP Status: ${regRes.status}`);
  console.log(`Registration Response: ${regRes.data}`);
  const regParsed = JSON.parse(regRes.data);

  if (regRes.status === 201 && regParsed.success) {
    console.log('✅ Operator Registration: PASS (HTTP 201 Created)');
  } else {
    console.log('❌ Operator Registration: FAILED');
  }

  console.log('\n=== TEST 2: DUPLICATE MOBILE REGISTRATION HANDLING ===');
  const dupRes = await post('http://localhost:8080/api/operators/register', newOperatorPayload);
  console.log(`Duplicate Registration HTTP Status: ${dupRes.status}`);
  console.log(`Duplicate Response: ${dupRes.data}`);
  if (dupRes.status >= 400) {
    console.log('✅ Duplicate Mobile Handling: PASS (Enforced and rejected by backend)');
  }

  console.log('\n=== TEST 3: OPERATOR LOGIN WITH SEEDED TEST CREDENTIALS ===');
  const seededLoginRes = await post('http://localhost:8080/api/operators/login', {
    mobileNumber: '9876543220',
    password: 'Operator@123'
  });
  console.log(`Seeded Operator Login HTTP Status: ${seededLoginRes.status}`);
  const seededParsed = JSON.parse(seededLoginRes.data);
  if (seededLoginRes.status === 200 && seededParsed.data?.accessToken) {
    console.log(`✅ Seeded Operator Login: PASS | User: ${seededParsed.data.operator.fullName} | Token acquired`);
  }

  console.log('\n=== TEST 4: VERIFYING ALL FRONTEND AUTH ROUTES ON PORT 5174 ===');
  const routes = [
    '/auth/operator',
    '/login/operator',
    '/register/operator',
    '/operator/dashboard'
  ];

  for (const r of routes) {
    const res = await get('http://localhost:5174' + r);
    console.log(`Frontend Route ${r.padEnd(22)} -> HTTP ${res.status}`);
  }
}

run().catch(console.error);
