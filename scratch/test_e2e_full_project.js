const http = require('http');

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

async function run() {
  console.log('=== 1. VERIFYING ALL FRONTEND ROUTES (PORT 5174) ===');
  const frontendRoutes = [
    { name: 'Home Page', route: '/' },
    { name: 'Unified Login', route: '/login' },
    { name: 'Farmer Registration', route: '/register' },
    { name: 'Partner Registration', route: '/register/partner' },
    { name: 'Operator Auth Landing', route: '/auth/operator' },
    { name: 'Operator Login', route: '/login/operator' },
    { name: 'Operator Registration', route: '/register/operator' },
    { name: 'Operator Dashboard', route: '/operator/dashboard' },
    { name: 'Operator Assigned Jobs', route: '/operator/jobs' },
    { name: 'Operator Job #1 Details', route: '/operator/jobs/1' },
    { name: 'Operator Profile', route: '/operator/profile' },
    { name: 'Operator Earnings', route: '/operator/earnings' },
    { name: 'Operator Ratings', route: '/operator/ratings' },
    { name: 'Operator Job History', route: '/operator/history' },
    { name: 'Operator Notifications', route: '/operator/notifications' }
  ];

  let frontendAllPass = true;
  for (const r of frontendRoutes) {
    const res = await get('http://localhost:5174' + r.route);
    const pass = res.status === 200 && res.data.includes('<div id="root">');
    console.log(`[${pass ? 'PASS' : 'FAIL'}] ${r.name.padEnd(26)} (${r.route.padEnd(24)}) -> HTTP ${res.status}`);
    if (!pass) frontendAllPass = false;
  }

  console.log('\n=== 2. OPERATOR AUTHENTICATION (PORT 8080) ===');
  const loginRes = await post('http://localhost:8080/api/operators/login', {
    mobileNumber: '9876543220',
    password: 'Operator@123'
  });
  console.log(`Operator Login HTTP: ${loginRes.status}`);
  const loginData = JSON.parse(loginRes.data);
  const token = loginData.data?.accessToken;
  const operator = loginData.data?.operator;
  console.log(`Logged in Operator: ${operator?.fullName} (ID: ${operator?.id}, Status: ${operator?.status})`);

  console.log('\n=== 3. VERIFYING ALL OPERATOR API ENDPOINTS (PORT 8080) ===');
  const authHeader = { 'Authorization': `Bearer ${token}` };

  const endpoints = [
    { name: 'Dashboard Metrics', url: 'http://localhost:8080/api/operators/dashboard/metrics' },
    { name: 'Earnings Summary', url: 'http://localhost:8080/api/operators/earnings/summary' },
    { name: 'Ratings Summary', url: 'http://localhost:8080/api/operators/me/ratings/summary' },
    { name: 'Reviews List', url: 'http://localhost:8080/api/operators/me/reviews?page=0&size=10' },
    { name: 'Profile Details', url: 'http://localhost:8080/api/operators/profile' },
    { name: 'Assigned Jobs', url: 'http://localhost:8080/api/operators/jobs/assigned?page=0&size=10' },
    { name: 'Single Job #1', url: 'http://localhost:8080/api/operators/jobs/1' },
    { name: 'Job #1 Earnings', url: 'http://localhost:8080/api/operators/jobs/1/earnings' },
    { name: 'Earnings History', url: 'http://localhost:8080/api/operators/earnings/history?page=0&size=10' },
    { name: 'Job History', url: 'http://localhost:8080/api/operators/jobs/history?page=0&size=10' },
    { name: 'History Summary', url: 'http://localhost:8080/api/operators/jobs/history/summary' },
    { name: 'Notifications List', url: 'http://localhost:8080/api/notifications?role=OPERATOR&id=1' }
  ];

  let apisAllPass = true;
  for (const ep of endpoints) {
    const res = await get(ep.url, authHeader);
    const parsed = JSON.parse(res.data);
    const pass = res.status === 200 && (parsed.success || parsed.content || Array.isArray(parsed));
    console.log(`[${pass ? 'PASS' : 'FAIL'}] API: ${ep.name.padEnd(25)} -> HTTP ${res.status}`);
    if (!pass) apisAllPass = false;
  }

  console.log('\n=== 4. SUMMARY OF VERIFICATION ===');
  console.log(`Frontend Routes Verified: ${frontendRoutes.length} | All PASS: ${frontendAllPass}`);
  console.log(`Backend APIs Verified:    ${endpoints.length} | All PASS: ${apisAllPass}`);
}

run().catch(console.error);
