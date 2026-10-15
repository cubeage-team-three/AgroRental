# Manage Farmers Feature - Quick Reference Guide

**Implementation Date:** 2026-09-11  
**Status:** ✅ Complete & Ready for Testing

---

## 📦 FILES MODIFIED/CREATED

### Backend (Java/Spring Boot)

| File | Status | Changes |
|------|--------|---------|
| `AdminFarmerController.java` | ✅ CREATED | New controller with 2 endpoints (GET, PUT) |
| `FarmerService.java` | ✅ MODIFIED | Added 3 new methods for admin operations |
| `FarmerRepository.java` | ✅ MODIFIED | Added pagination & search capabilities |

### Frontend (React)

| File | Status | Changes |
|------|--------|---------|
| `ManageFarmers.jsx` | ✅ MODIFIED | Complete data table implementation |

---

## 🎯 CORE FEATURES

### ✅ Implemented
- [x] Data table display with farmer information
- [x] Dark theme with green accents (matches admin layout)
- [x] Real-time search (name, phone, email)
- [x] Pagination (Previous/Next + page numbers)
- [x] Status badges with color coding
- [x] View Profile button (all farmers)
- [x] Approve KYC button (KYC_COMPLETED only)
- [x] Loading states and error handling
- [x] Debounced search (500ms)
- [x] API integration with JWT auth
- [x] Security: @PreAuthorize("hasRole('ADMIN')")

### 🔄 Backend API Endpoints

#### GET /api/admin/farmers
```bash
# Get all farmers (paginated)
curl -H "Authorization: Bearer JWT_TOKEN" \
  "http://localhost:8080/api/admin/farmers?page=0&size=10"

# Search farmers
curl -H "Authorization: Bearer JWT_TOKEN" \
  "http://localhost:8080/api/admin/farmers?page=0&size=10&search=Rajesh"
```

**Response:** `Page<Farmer>` with pagination metadata

#### PUT /api/admin/farmers/{id}/approve-kyc
```bash
# Approve farmer KYC
curl -X PUT \
  -H "Authorization: Bearer JWT_TOKEN" \
  "http://localhost:8080/api/admin/farmers/1/approve-kyc"
```

**Response:** Updated `Farmer` object with `accountStatus = ACTIVE`

---

## 🏗️ CLASS STRUCTURE

### AdminFarmerController.java
```java
@RestController
@RequestMapping("/api/admin/farmers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmerController {
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Farmer>>> getAllFarmers(
            @RequestParam(required = false) String search,
            Pageable pageable) { ... }
    
    @PutMapping("/{farmerId}/approve-kyc")
    public ResponseEntity<ApiResponse<Farmer>> approveKyc(
            @PathVariable Long farmerId) { ... }
}
```

### FarmerService.java (Added Methods)
```java
@Service
public class FarmerService {
    
    @Transactional(readOnly = true)
    public Page<Farmer> getAllFarmers(Pageable pageable) { ... }
    
    @Transactional(readOnly = true)
    public Page<Farmer> searchFarmers(String search, Pageable pageable) { ... }
    
    @Transactional
    public Farmer approveFarmerKyc(Long farmerId) { ... }
}
```

### FarmerRepository.java (Added Methods)
```java
@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long>, 
                                          JpaSpecificationExecutor<Farmer> {
    
    @Query("SELECT f FROM Farmer f WHERE " +
            "LOWER(f.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "f.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Farmer> searchByNameOrPhoneOrEmail(@Param("search") String search, 
                                           Pageable pageable);
}
```

### ManageFarmers.jsx (React Component)
```jsx
export default function ManageFarmers() {
  const [farmers, setFarmers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  
  // Fetch, search, and approve functions
  const fetchFarmers = async (page, search) { ... }
  const handleApproveKyc = async (farmerId) { ... }
  const getStatusBadge = (status) { ... }
  
  return ( /* Data table with search, pagination, actions */ )
}
```

---

## 📊 DATA FLOW

### Fetch Farmers Flow
```
User clicks page → fetchFarmers(page, searchTerm)
    ↓
GET /api/admin/farmers?page=X&size=10&search=Y
    ↓
AdminFarmerController.getAllFarmers()
    ↓
FarmerService.getAllFarmers() or searchFarmers()
    ↓
FarmerRepository.findAll() or searchByNameOrPhoneOrEmail()
    ↓
MySQL Query → Page<Farmer>
    ↓
setFarmers(response.data.content)
    ↓
Render table with farmers data
```

### Approve KYC Flow
```
User clicks "Approve KYC" → handleApproveKyc(farmerId)
    ↓
PUT /api/admin/farmers/{farmerId}/approve-kyc
    ↓
AdminFarmerController.approveKyc(farmerId)
    ↓
FarmerService.approveFarmerKyc(farmerId)
    ↓
Verify farmer exists & status is KYC_COMPLETED
    ↓
Update status to ACTIVE
    ↓
Save & return updated Farmer
    ↓
Update local state: setFarmers([...updated...])
    ↓
Re-render with new status (green badge, button hidden)
```

---

## 🎨 UI COMPONENTS

### Table Columns
| Column | Data | Example |
|--------|------|---------|
| Name & ID | `fullName` + `id` | Rajesh Kumar (ID: 1) |
| Contact | `mobileNumber` + `email` | 9876543210 / rajesh@ex.com |
| Status | `accountStatus` badge | KYC_COMPLETED (blue) |
| Actions | Buttons | [👁️] [✓ Approve KYC] |

### Status Badges
| Status | Color | Label |
|--------|-------|-------|
| `ACTIVE` | Green (bg-green-900) | Active |
| `KYC_COMPLETED` | Blue (bg-blue-900) | KYC Completed |
| `OTP_VERIFIED` | Yellow (bg-yellow-900) | OTP Verified |
| `PENDING_OTP` | Gray (bg-gray-700) | Pending OTP |

### Buttons
| Action | Condition | Style |
|--------|-----------|-------|
| View Profile | All farmers | Gray icon button |
| Approve KYC | Only `KYC_COMPLETED` | Green button with checkmark |

---

## ⚙️ CONFIGURATION

### Backend Configuration
- **Security:** JWT authentication + @PreAuthorize
- **Pagination:** Default 10 records per page
- **Search:** Case-insensitive for name/email, case-sensitive for phone
- **Transactions:** Read-only for queries, write for approvals

### Frontend Configuration
- **API Base URL:** Defined in `apiClient.js` (default: http://localhost:8080)
- **Search Debounce:** 500ms
- **Page Size:** 10 records
- **Theme:** Dark (gray-900/800) with green-500 accents

---

## ✅ TESTING QUICK START

### Manual Testing
1. Start backend: `cd backend && ./mvnw spring-boot:run`
2. Start database: `docker-compose up -d`
3. Start frontend: `cd frontend && npm run dev`
4. Login as admin
5. Navigate to Manage Farmers page
6. Test search, pagination, and approve actions

### Key Test Cases
- [ ] Display all farmers (paginated)
- [ ] Search by name/phone/email
- [ ] Verify status badges display correctly
- [ ] Approve KYC for farmer with KYC_COMPLETED status
- [ ] Verify error when approving non-KYC_COMPLETED farmer
- [ ] Verify pagination navigation works
- [ ] Verify loading states appear during operations

---

## 🔐 SECURITY CHECKLIST

- [x] JWT authentication required
- [x] Admin role enforcement (@PreAuthorize)
- [x] No sensitive data exposed
- [x] SQL injection prevention (parameterized queries)
- [x] Input validation (search, farmer ID)
- [x] Proper HTTP status codes (401, 403, 404)
- [x] Error messages don't reveal system details

---

## 📝 ERROR HANDLING

### Frontend
```
Success: Update UI, clear error
400 Bad Request: Display error message (e.g., "Cannot approve KYC...")
401 Unauthorized: Display "Please login again"
403 Forbidden: Display "Access denied"
404 Not Found: Display "Farmer not found"
Network Error: Display "Failed to fetch farmers"
```

### Backend
```
IllegalArgumentException → 404 Not Found
IllegalStateException → 400 Bad Request
Authentication Error → 401 Unauthorized
Authorization Error → 403 Forbidden
```

---

## 🎯 NEXT STEPS

1. **Deploy to staging:** Test with realistic data volume
2. **Load test:** Verify performance with 1000+ farmers
3. **Add detail page:** Implement farmer profile view
4. **Add bulk actions:** Select multiple farmers for batch approval
5. **Add audit logging:** Log all admin actions
6. **Add email notifications:** Notify farmers of KYC approval
7. **Add export:** Export farmer list to CSV/Excel

---

## 📞 COMMON ISSUES & SOLUTIONS

| Issue | Solution |
|-------|----------|
| No farmers displayed | Verify backend is running, database has data |
| Search not working | Check network tab, verify URL parameters |
| Approve button missing | Farmer status must be `KYC_COMPLETED` |
| 401 Unauthorized | Login again to get new JWT token |
| 403 Forbidden | User must have ADMIN role |
| 404 Farmer not found | Verify farmer ID exists in database |

---

**Ready to Deploy!** ✅

All code follows AgroRental Platform conventions and best practices.

