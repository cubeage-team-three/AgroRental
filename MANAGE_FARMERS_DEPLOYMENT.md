# Manage Farmers Feature - Deployment & Architecture Guide

**Date:** 2026-09-11  
**Version:** 1.0  
**Environment:** Production Ready

---

## 🏗️ SYSTEM ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ADMIN BROWSER                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │            ManageFarmers.jsx (React Component)              │   │
│  │                                                             │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │  Search Bar                                          │  │   │
│  │  │  [🔍 Search by name, phone, email...] (500ms delay)  │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │  Farmers Data Table                                  │  │   │
│  │  │  ┌─────────────────────────────────────────────────┐ │  │   │
│  │  │  │ Name & ID │ Contact │ Status │ Actions         │ │  │   │
│  │  │  ├─────────────────────────────────────────────────┤ │  │   │
│  │  │  │ Rajesh    │ 987... │ 🟦     │ [👁️] [✓Approve] │ │  │   │
│  │  │  │ Priya     │ 912... │ 🟩     │ [👁️]             │ │  │   │
│  │  │  │ ...       │ ...    │ ...    │ ...              │ │  │   │
│  │  │  └─────────────────────────────────────────────────┘ │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │  Pagination: [◄] [1] [2] [3] [...] [►]              │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │ HTTP      │ REST      │ AXIOS
                    ▼           ▼           ▼
        ┌───────────────────────────────────────────────┐
        │         AXIOS HTTP CLIENT                     │
        │  (apiClient.js - with JWT auth headers)       │
        └───────────────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
    GET /api/admin/farmers              PUT /api/admin/farmers/{id}/approve-kyc
        ?page=0&size=10                         
        ?search=optional                │
                │                       │
                └───────────┬───────────┘
                            │
                    ┌───────▼──────────┐
                    │ Spring Boot API  │
                    │ (Port: 8080)     │
                    └──────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │  AdminFarmerController                                  │
    │  ┌─────────────────────────────────────────────────┐   │
    │  │ @GetMapping                                     │   │
    │  │ getAllFarmers(search?, Pageable)                │   │
    │  │ → farmerService.getAllFarmers() or             │   │
    │  │   farmerService.searchFarmers()                │   │
    │  │ → return ApiResponse<Page<Farmer>>             │   │
    │  │                                                 │   │
    │  │ @PutMapping("/{id}/approve-kyc")               │   │
    │  │ approveKyc(farmerId)                            │   │
    │  │ → farmerService.approveFarmerKyc(id)           │   │
    │  │ → return ApiResponse<Farmer>                   │   │
    │  └─────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │  FarmerService                                          │
    │  ┌─────────────────────────────────────────────────┐   │
    │  │ getAllFarmers(Pageable)                         │   │
    │  │ → farmerRepository.findAll(pageable)           │   │
    │  │                                                 │   │
    │  │ searchFarmers(search, Pageable)                 │   │
    │  │ → farmerRepository.searchByName...(...) │   │
    │  │                                                 │   │
    │  │ approveFarmerKyc(farmerId)                      │   │
    │  │ → Validate farmer exists                       │   │
    │  │ → Validate status is KYC_COMPLETED             │   │
    │  │ → Update status to ACTIVE                      │   │
    │  │ → Save & return farmer                         │   │
    │  └─────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │  FarmerRepository (JPA)                                 │
    │  ┌─────────────────────────────────────────────────┐   │
    │  │ findAll(Pageable)                               │   │
    │  │ SELECT * FROM farmers LIMIT ? OFFSET ?          │   │
    │  │                                                 │   │
    │  │ searchByNameOrPhoneOrEmail(search, Pageable)    │   │
    │  │ SELECT * FROM farmers WHERE                     │   │
    │  │   LOWER(fullName) LIKE ? OR                    │   │
    │  │   mobileNumber LIKE ? OR                       │   │
    │  │   LOWER(email) LIKE ?                          │   │
    │  │ LIMIT ? OFFSET ?                                │   │
    │  │                                                 │   │
    │  │ (Spring Data generates & executes)             │   │
    │  └─────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────┘
                            │
                    ┌───────▼──────────┐
                    │  MySQL Database  │
                    │  farmers table   │
                    │  (Port: 3306)    │
                    └──────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │  farmers table (MySQL)                                  │
    │  ┌──────────────────────────────────────────────────┐   │
    │  │ id │ user_id │ fullName │ mobileNumber │ email  │   │
    │  │ accountStatus │ address │ profileImage │ ...    │   │
    │  │ createdAt │ updatedAt │ active │ version │       │   │
    │  ├──────────────────────────────────────────────────┤   │
    │  │ 1  │ 10 │ Rajesh Kumar │ 9876543210 │ r@ex   │   │
    │  │ KYC_COMPLETED │ ...                              │   │
    │  │ 2  │ 11 │ Priya Singh  │ 9123456789 │ p@ex   │   │
    │  │ ACTIVE │ ...                                      │   │
    │  │ ...                                              │   │
    │  └──────────────────────────────────────────────────┘   │
    │  Indexes:                                              │
    │  - idx_farmer_fullname (fullName)                     │
    │  - idx_farmer_phone (mobileNumber)                    │
    │  - idx_farmer_email (email)                           │
    └─────────────────────────────────────────────────────────┘
```

---

## 📋 REQUEST-RESPONSE FLOW DIAGRAMS

### Flow 1: Get All Farmers (Paginated)

```
[Client] ─────────────┐
                      │
  GET /api/admin/farmers?page=0&size=10
  Headers: Authorization: Bearer JWT_TOKEN
                      │
                      ▼
        [AdminFarmerController]
                      │
                      ├─→ @PreAuthorize checks user is ADMIN ✓
                      │
                      ├─→ Call FarmerService.getAllFarmers(pageable)
                      │
                      ▼
        [FarmerService]
                      │
                      ├─→ @Transactional(readOnly=true)
                      │
                      ├─→ Call FarmerRepository.findAll(pageable)
                      │
                      ▼
        [FarmerRepository - Spring Data]
                      │
                      ├─→ Generate SQL: SELECT * FROM farmers LIMIT 10 OFFSET 0
                      │
                      ▼
        [MySQL Database]
                      │
                      ├─→ Execute query
                      │
                      ├─→ Return 10 farmer records
                      │
                      ▼
        [FarmerRepository]
                      │
                      ├─→ Map ResultSet to Farmer entities
                      │
                      ├─→ Return Page<Farmer> with metadata
                      │
                      ▼
        [FarmerService]
                      │
                      ├─→ Return Page<Farmer>
                      │
                      ▼
        [AdminFarmerController]
                      │
                      ├─→ Wrap in ApiResponse<Page<Farmer>>
                      │
                      ├─→ Return 200 OK
                      │
                      ▼
       [HTTP Response]
{
  "success": true,
  "message": "Farmers retrieved successfully",
  "data": {
    "content": [...10 farmer objects...],
    "totalPages": 3,
    "totalElements": 25,
    "pageable": {...}
  }
}
                      │
                      ▼
                  [Client]
                      │
                      ├─→ Parse response.data.data.content
                      │
                      ├─→ Update setFarmers([...])
                      │
                      ├─→ Re-render table with farmers
                      │
                      ▼
              [Table displays farmers]
```

### Flow 2: Search Farmers

```
[User] ─── types "Rajesh" in search box ───┐
                                            │
                                 500ms delay (debounce)
                                            │
                                            ▼
[Client - ManageFarmers.jsx]
                                            │
  ├─→ useEffect triggers on searchTerm change
  │
  ├─→ setLoading(true)
  │
  ├─→ Call fetchFarmers(page=0, search="Rajesh")
                                            │
  ─────────────────────────────────────────┘
                                            │
    GET /api/admin/farmers?page=0&size=10&search=Rajesh
    Headers: Authorization: Bearer JWT_TOKEN
                                            │
                                            ▼
[AdminFarmerController.getAllFarmers()]
                                            │
  ├─→ @PreAuthorize checks ADMIN ✓
  │
  ├─→ search parameter is not empty
  │
  ├─→ Call FarmerService.searchFarmers("Rajesh", pageable)
                                            │
                                            ▼
[FarmerService.searchFarmers()]
                                            │
  ├─→ @Transactional(readOnly=true)
  │
  ├─→ Call FarmerRepository.searchByNameOrPhoneOrEmail("Rajesh", pageable)
                                            │
                                            ▼
[FarmerRepository.searchByNameOrPhoneOrEmail()]
                                            │
  ├─→ Execute custom @Query:
  │   SELECT f FROM Farmer f WHERE
  │     LOWER(f.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
  │     f.mobileNumber LIKE CONCAT('%', :search, '%') OR
  │     LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%'))
                                            │
                                            ▼
[MySQL Database]
                                            │
  ├─→ Execute parameterized query
  │   (search = "Rajesh", parameters = ["%rajesh%", "%Rajesh%", "%rajesh%"])
  │
  ├─→ Return matching farmers
  │   (e.g., farmer with fullName = "Rajesh Kumar")
                                            │
                                            ▼
[Response flows back to Client]
                                            │
  ├─→ setFarmers([farmer with name="Rajesh Kumar"])
  │
  ├─→ setTotalPages(1)
  │
  ├─→ setLoading(false)
  │
  ├─→ Re-render table with search results
                                            │
                                            ▼
[User sees filtered results in table]
```

### Flow 3: Approve Farmer KYC

```
[User] ─── clicks "Approve KYC" button for farmer ID 1 ───┐
                                                           │
                                     [handleApproveKyc(1)]
                                                           │
                                  setApprovingId(1)
                                  [Show loading spinner]
                                                           │
                                                           ▼
    PUT /api/admin/farmers/1/approve-kyc
    Authorization: Bearer JWT_TOKEN
                                                           │
                                                           ▼
        [AdminFarmerController.approveKyc(farmerId=1)]
                                                           │
            ├─→ @PreAuthorize checks ADMIN ✓
            │
            ├─→ Call FarmerService.approveFarmerKyc(1)
                                                           │
                                                           ▼
            [FarmerService.approveFarmerKyc(1)]
                                                           │
                ├─→ @Transactional
                │
                ├─→ Find farmer by ID 1
                │   └─→ FarmerRepository.findById(1)
                │
                ├─→ Check if farmer exists ✓
                │
                ├─→ Check if status == "KYC_COMPLETED" ✓
                │   (If not, throw IllegalStateException)
                │
                ├─→ Set status to "ACTIVE"
                │
                ├─→ Save farmer
                │   └─→ FarmerRepository.save(farmer)
                │
                ├─→ Return updated farmer
                                                           │
                                                           ▼
            [AdminFarmerController]
                                                           │
                ├─→ Wrap in ApiResponse<Farmer>
                │
                ├─→ Return 200 OK with updated farmer
                                                           │
                                                           ▼
        [HTTP Response]
{
  "success": true,
  "message": "Farmer KYC approved successfully",
  "data": {
    "id": 1,
    "fullName": "Rajesh Kumar",
    "accountStatus": "ACTIVE",
    ...
  }
}
                                                           │
                                                           ▼
                                         [Client receives response]
                                                           │
                    ├─→ Check if response.data.success ✓
                    │
                    ├─→ Update farmers array:
                    │   farmer.id=1: accountStatus="ACTIVE"
                    │
                    ├─→ setApprovingId(null)
                    │   [Hide loading spinner]
                    │
                    ├─→ Re-render table:
                    │   - Status badge changes to green ✓
                    │   - "Approve KYC" button disappears
                    │   - Table row highlights update
                                                           │
                                                           ▼
                                    [User sees updated farmer]
```

---

## 🚀 DEPLOYMENT STEP-BY-STEP GUIDE

### Phase 1: Pre-Deployment Checks

#### 1.1 Environment Verification
```bash
# Check Java version
java -version  # Should be 21+

# Check Maven
mvn -version   # Should be 3.8+

# Check Node.js
node -version  # Should be 18+
npm -version   # Should be 9+

# Check MySQL
mysql --version  # Should be 8.0+

# Check Docker
docker --version
docker-compose --version
```

#### 1.2 Database Setup
```bash
# Start database
docker-compose up -d

# Verify MySQL is running
mysql -u root -p -e "SELECT 1;"

# Create recommended indexes
mysql -u root -p agrorental_db <<EOF
CREATE INDEX idx_farmer_fullname ON farmers(full_name);
CREATE INDEX idx_farmer_phone ON farmers(mobile_number);
CREATE INDEX idx_farmer_email ON farmers(email);
EOF
```

---

### Phase 2: Backend Deployment

#### 2.1 File Placement
```bash
# Create directory structure if needed
mkdir -p backend/src/main/java/com/agrorental/admin/controller

# Copy files
cp MANAGE_FARMERS_CODE_SNIPPETS.md/AdminFarmerController.java \
   backend/src/main/java/com/agrorental/admin/controller/

# Verify FarmerService.java exists and copy updated version
cp MANAGE_FARMERS_CODE_SNIPPETS.md/FarmerService.java \
   backend/src/main/java/com/agrorental/farmer/service/

# Verify FarmerRepository.java exists and update
cp MANAGE_FARMERS_CODE_SNIPPETS.md/FarmerRepository.java \
   backend/src/main/java/com/agrorental/farmer/repository/
```

#### 2.2 Compilation & Testing
```bash
# Navigate to backend directory
cd backend

# Clean and compile
./mvnw clean compile
# ✅ Should complete with BUILD SUCCESS

# Run unit tests
./mvnw test
# ✅ Should pass all tests

# Build JAR
./mvnw clean package
# ✅ Should create target/backend-VERSION.jar
```

#### 2.3 Local Validation
```bash
# Start Spring Boot app
./mvnw spring-boot:run
# ✅ Should show: "Started [AppName] in X.XXX seconds"

# Verify endpoints are available
curl -H "Authorization: Bearer DUMMY_TOKEN" \
  http://localhost:8080/api/admin/farmers
# ✅ Should return 200 OK (or 401 if JWT validation fails)

# Stop app (Ctrl+C)
```

---

### Phase 3: Frontend Deployment

#### 3.1 File Placement
```bash
# Copy updated component
cp MANAGE_FARMERS_CODE_SNIPPETS.md/ManageFarmers.jsx \
   frontend/src/pages/admin/

# Verify dependencies
cd frontend
npm ls lucide-react
# If not installed:
npm install lucide-react
```

#### 3.2 Build & Testing
```bash
# Navigate to frontend
cd frontend

# Install/verify dependencies
npm install

# Build for production
npm run build
# ✅ Should create dist/ directory with optimized bundle

# Start dev server for testing
npm run dev
# ✅ Should start on http://localhost:5173

# In another terminal, test component loads
curl http://localhost:5173/admin/farmers
# ✅ Should return HTML with ManageFarmers component
```

#### 3.3 Component Testing
```bash
# Open browser
open http://localhost:5173

# Login as admin user

# Navigate to Manage Farmers page
# ✅ Should display:
#   - Search bar
#   - Farmers table
#   - Pagination controls
#   - Status badges
#   - Action buttons
```

---

### Phase 4: Integration Testing

#### 4.1 API Integration Test
```bash
# Terminal 1: Start backend
cd backend && ./mvnw spring-boot:run

# Terminal 2: Start MySQL (if not already running)
docker-compose up -d

# Terminal 3: Test endpoints
# Test 1: Get all farmers
curl -X GET \
  -H "Authorization: Bearer $(get_admin_jwt_token)" \
  "http://localhost:8080/api/admin/farmers?page=0&size=10"
# ✅ Should return 200 OK with farmers list

# Test 2: Search farmers
curl -X GET \
  -H "Authorization: Bearer $(get_admin_jwt_token)" \
  "http://localhost:8080/api/admin/farmers?search=Rajesh&page=0&size=10"
# ✅ Should return 200 OK with filtered results

# Test 3: Approve KYC
curl -X PUT \
  -H "Authorization: Bearer $(get_admin_jwt_token)" \
  "http://localhost:8080/api/admin/farmers/1/approve-kyc"
# ✅ Should return 200 OK with updated farmer status

# Test 4: Error case (farmer not found)
curl -X PUT \
  -H "Authorization: Bearer $(get_admin_jwt_token)" \
  "http://localhost:8080/api/admin/farmers/99999/approve-kyc"
# ✅ Should return 404 Not Found
```

#### 4.2 Full Stack Test
```
✅ Backend running on :8080
✅ Frontend running on :5173
✅ Database accessible on :3306
✅ User logged in as ADMIN

Steps:
1. Navigate to Manage Farmers page
2. Verify table displays 10 farmers
3. Test search functionality
4. Test pagination (if > 10 farmers)
5. Click Approve KYC button for KYC_COMPLETED farmer
6. Verify status changes to ACTIVE
7. Verify button disappears
8. Test error cases
```

---

### Phase 5: Staging Deployment

#### 5.1 Configuration Update
```bash
# Update application-staging.properties
vi backend/src/main/resources/application-staging.properties

# Key settings for staging:
server.port=8080
spring.datasource.url=jdbc:mysql://staging-db:3306/agrorental_staging
spring.jpa.hibernate.ddl-auto=validate
logging.level.root=INFO
logging.level.com.agrorental=DEBUG
```

#### 5.2 Docker Deployment (Optional)
```bash
# Build Docker image
docker build -f backend/Dockerfile -t agrorental-api:1.0 .

# Run with docker-compose
docker-compose -f docker-compose.staging.yml up -d

# Verify service is running
curl http://localhost:8080/health
```

#### 5.3 Staging Validation
```bash
# Run full test suite
./mvnw verify

# Run integration tests
./mvnw integration-test

# Performance test (load testing)
# Using JMeter or similar tool
```

---

### Phase 6: Production Deployment

#### 6.1 Production Configuration
```bash
# Create application-prod.properties
vi backend/src/main/resources/application-prod.properties

# Critical settings:
server.port=8080
spring.datasource.url=jdbc:mysql://prod-db:3306/agrorental_prod
spring.jpa.hibernate.ddl-auto=validate
logging.level.root=WARN
logging.level.com.agrorental=INFO
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
```

#### 6.2 Pre-Production Checklist
- [ ] Code reviewed by team lead
- [ ] Security scan completed (OWASP)
- [ ] Performance tested with production-like data volume
- [ ] Database backups created
- [ ] Rollback plan documented
- [ ] Monitoring/logging configured
- [ ] Error tracking (Sentry) setup
- [ ] CDN/caching configured (if needed)

#### 6.3 Production Deployment
```bash
# Build production bundle
./mvnw clean package -P production

# Deploy JAR to production server
scp target/backend-1.0.jar prod-server:/opt/app/

# On production server:
ssh prod-server

cd /opt/app

# Stop old version
systemctl stop agrorental

# Backup database
mysqldump -u root -p agrorental_prod > /backup/agrorental_prod_$(date +%Y%m%d).sql

# Start new version
java -jar backend-1.0.jar --spring.profiles.active=prod

# Verify service
curl http://localhost:8080/health
```

#### 6.4 Post-Deployment Verification
```bash
# Check service health
curl https://api.agrorental.com/health

# Verify all endpoints are working
curl -H "Authorization: Bearer PROD_JWT_TOKEN" \
  https://api.agrorental.com/api/admin/farmers

# Monitor logs
tail -f /var/log/agrorental/app.log

# Check error tracking (Sentry)
# Should show 0 errors

# Monitor performance metrics
# Database queries should be < 200ms
# API responses should be < 500ms
```

---

## 🔄 ROLLBACK PROCEDURE

If issues occur in production:

```bash
# Stop current version
systemctl stop agrorental

# Restore database from backup
mysql -u root -p agrorental_prod < /backup/agrorental_prod_YYYYMMDD.sql

# Deploy previous working version
java -jar backend-PREVIOUS_VERSION.jar --spring.profiles.active=prod

# Verify service is working
curl https://api.agrorental.com/health

# Notify team
# Document incident
```

---

## 📊 DEPLOYMENT CHECKLIST

### Pre-Deployment
- [ ] Code peer-reviewed
- [ ] Unit tests passing (100% of new code)
- [ ] Integration tests passing
- [ ] Security review passed
- [ ] Database migration tested
- [ ] Documentation updated
- [ ] Rollback plan documented
- [ ] Team notified

### Deployment
- [ ] Database indexes created
- [ ] Backend code deployed
- [ ] Frontend code deployed
- [ ] Environment variables configured
- [ ] SSL certificates configured (prod)
- [ ] Monitoring configured
- [ ] Error tracking configured

### Post-Deployment
- [ ] Health checks passing
- [ ] All endpoints responding
- [ ] Error logs clear
- [ ] Performance metrics normal
- [ ] User testing completed
- [ ] Monitoring active
- [ ] Documentation updated

---

## 📞 SUPPORT CONTACTS

**For Technical Issues:**
- Backend Lead: [contact]
- Frontend Lead: [contact]
- DevOps Lead: [contact]

**For Production Incidents:**
- On-Call: [contact]
- Escalation: [contact]

---

**Deployment Complete!** ✅

All systems online and monitoring active.

