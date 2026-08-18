# Machine Management / Equipment Management Module — Technical Handoff & API Documentation

**Project:** AgroRental  
**Package:** `com.agrorental.equipment`  
**Frontend:** React 19 + Vite 8 + TailwindCSS 4 + React Router 7  
**Backend:** Java 21 + Spring Boot 4.1 + Spring Data JPA + Spring Security + MySQL 8  
**Status:** **READY FOR PRODUCTION DEMO**  
**Build Status:** Backend Clean Verify **SUCCESS** (38/38 Active Tests Passed) \| Frontend Vite Build **SUCCESS** (built in 244ms)  

---

## 1. Module Architecture & Data Flow

```
Frontend UI (React 19 / Vite 8)
       ↓
equipmentService.js (API Abstraction)
       ↓
apiClient.js (Fetch + X-Partner-Id Header + ApiResponse Unwrapping)
       ↓
HTTP REST Controller (EquipmentController @ /api/equipment)
       ↓
Security Filter Chain (SecurityConfig Stateless Session & Permitted Routes)
       ↓
EquipmentService (Business Logic, Ownership Validation, Invariants, & Transactions)
       ↓
EquipmentMapper (Pure DTO ↔ Entity Transformation & Display Ordering)
       ↓
EquipmentRepository & EquipmentSpecification (JPA Criteria API Database-Side Querying)
       ↓
Entity Layer (Equipment, EquipmentImage extending BaseEntity with @Version Optimistic Locking)
       ↓
MySQL Database (equipment & equipment_image tables)
```

---

## 2. File Inventory

### Backend Codebase (`com.agrorental.equipment` & `com.agrorental.common`):
- **Entities:**
  - `com.agrorental.common.entity.BaseEntity`: Abstract superclass defining `@Id Long id`, `@CreatedDate LocalDateTime createdAt`, `@LastModifiedDate LocalDateTime updatedAt`, `boolean active`, and `@Version Long version`.
  - `com.agrorental.equipment.entity.Equipment`: Aggregate Root representing machinery listings.
  - `com.agrorental.equipment.entity.EquipmentImage`: Child entity representing machinery photos.
- **DTO Layer (`com.agrorental.equipment.dto`):**
  - `EquipmentCreateRequest`: Request payload for creating new machinery.
  - `EquipmentUpdateRequest`: Request payload for modifying existing machinery.
  - `EquipmentImageRequest`: Nested DTO for image URLs and primary status.
  - `EquipmentResponse`: Full detail response payload.
  - `EquipmentSummaryResponse`: Compact card response payload for lists and search results.
  - `EquipmentImageResponse`: Image metadata payload.
  - `EquipmentSearchRequest`: Multi-criteria search query parameter holder.
- **Mappers & Persistence:**
  - `com.agrorental.equipment.mapper.EquipmentMapper`: Component mapping DTOs ↔ Entities.
  - `com.agrorental.equipment.repository.EquipmentRepository`: Spring Data JPA repository.
  - `com.agrorental.equipment.specification.EquipmentSpecification`: JPA Criteria API builder for dynamic search.
- **Service & Controller Layer:**
  - `com.agrorental.equipment.service.EquipmentService`: Core transactional service.
  - `com.agrorental.equipment.controller.EquipmentController`: REST controller exposing `/api/equipment`.
  - `com.agrorental.common.config.SecurityConfig`: Spring Security stateless chain and CORS settings.
  - `com.agrorental.common.exception.GlobalExceptionHandler`: `@RestControllerAdvice` mapping exceptions to standard `ApiResponse` responses.

### Frontend Codebase (`frontend/src/`):
- `services/apiClient.js`: Fetch wrapper with base URL injection and `X-Partner-Id` header propagation.
- `services/equipmentService.js`: Service layer wrapping all 12 backend endpoints.
- `utils/constants.js`: Category, fuel type, and availability enum labels, badge styles, and fallbacks.
- `pages/farmer/SearchEquipment.jsx`: Farmer machinery discovery and multi-criteria search screen.
- `pages/farmer/EquipmentDetails.jsx`: Detailed machinery specifications and photo gallery.
- `pages/partner/MyEquipment.jsx`: Partner equipment inventory management dashboard.
- `pages/partner/AddEquipment.jsx`: Equipment creation and edit form with 409 Conflict handling.
- `pages/partner/EquipmentAvailability.jsx`: Partner fleet availability status management dashboard.
- `pages/admin/ManageEquipment.jsx`: Admin equipment catalog oversight dashboard.

---

## 3. Data Contracts & Enums

### Domain Enums:
- `EquipmentCategory`: `TRACTOR`, `HARVESTER`, `TILLER`, `IRRIGATION`, `SEEDER`, `SPRAYER`, `OTHER`
- `FuelType`: `DIESEL`, `PETROL`, `ELECTRIC`, `HYBRID`, `MANUAL_HUMAN_POWERED`, `OTHER`
- `AvailabilityStatus`: `AVAILABLE`, `BOOKED`, `UNDER_MAINTENANCE`, `INACTIVE`

### Database Schema Mappings:
- **`equipment` Table:**
  - `id` (BIGINT, Primary Key, Auto Increment)
  - `name` (VARCHAR(100), NOT NULL)
  - `category` (VARCHAR(50), NOT NULL)
  - `brand` (VARCHAR(50), NOT NULL)
  - `model` (VARCHAR(50), NOT NULL)
  - `manufacturing_year` (INT, NOT NULL)
  - `capacity` (VARCHAR(50), NOT NULL)
  - `rental_price` (DECIMAL(10,2), NOT NULL)
  - `fuel_type` (VARCHAR(20), NOT NULL)
  - `description` (TEXT, NOT NULL)
  - `partner_id` (BIGINT, Foreign Key to `partner`, NOT NULL)
  - `location_address` (VARCHAR(255), NOT NULL)
  - `latitude` (DOUBLE, NOT NULL)
  - `longitude` (DOUBLE, NOT NULL)
  - `availability_status` (VARCHAR(30), NOT NULL)
  - `maintenance_notes` (TEXT, NULLABLE)
  - `is_disabled` (BOOLEAN, NOT NULL, DEFAULT FALSE)
  - `is_active` (BOOLEAN, NOT NULL, DEFAULT TRUE)
  - `created_at` (DATETIME, NOT NULL)
  - `updated_at` (DATETIME, NOT NULL)
  - `version` (BIGINT, Optimistic Lock Column)

- **`equipment_image` Table:**
  - `id` (BIGINT, Primary Key, Auto Increment)
  - `equipment_id` (BIGINT, Foreign Key to `equipment`, NOT NULL)
  - `image_url` (VARCHAR(500), NOT NULL)
  - `is_primary` (BOOLEAN, NOT NULL)
  - `display_order` (INT, NOT NULL)
  - `is_active` (BOOLEAN, NOT NULL, DEFAULT TRUE)
  - `created_at` (DATETIME, NOT NULL)
  - `updated_at` (DATETIME, NOT NULL)

---

## 4. Complete REST API Specification

### 1. Create Equipment (`POST /api/equipment`)
- **Headers:** `Content-Type: application/json`, `X-Partner-Id: 1`
- **Request Body:** `EquipmentCreateRequest`
- **Response:** `201 CREATED` with `ApiResponse<EquipmentResponse>`
- **Validation:** Name, category, brand, model, year, capacity, rental price > 0, fuel type, address required.

### 2. Get Equipment Details (`GET /api/equipment/{id}`)
- **Authorization:** Public / Farmer / Partner / Admin
- **Response:** `200 OK` with `ApiResponse<EquipmentResponse>`
- **Error:** `404 NOT FOUND` if machinery ID does not exist.

### 3. Update Equipment (`PUT /api/equipment/{id}`)
- **Headers:** `Content-Type: application/json`, `X-Partner-Id: <ownerPartnerId>`
- **Request Body:** `EquipmentUpdateRequest`
- **Response:** `200 OK` with `ApiResponse<EquipmentResponse>`
- **Guards & Behaviors:**
  - Validates partner ownership (returns `403 FORBIDDEN` if non-owner).
  - Rejects manual client setting of `availabilityStatus = BOOKED` (returns `400 BAD REQUEST`).
  - Enforces optimistic locking (returns `409 CONFLICT` if stale version updated).

### 4. Enable Equipment (`PUT /api/equipment/{id}/enable`)
- **Headers:** `X-Partner-Id: <ownerPartnerId>`
- **Behavior:** Sets `isDisabled = false`.
- **Response:** `200 OK` with `ApiResponse<EquipmentResponse>`

### 5. Disable Equipment (`PUT /api/equipment/{id}/disable`)
- **Headers:** `X-Partner-Id: <ownerPartnerId>`
- **Behavior:** Sets `isDisabled = true`.
- **Response:** `200 OK` with `ApiResponse<EquipmentResponse>`

### 6. Delete Equipment (`DELETE /api/equipment/{id}`)
- **Headers:** `X-Partner-Id: <ownerPartnerId>`
- **Behavior:** Soft-deactivates equipment (`active = false`).
- **Response:** `204 NO CONTENT`

### 7. Delete Equipment Image (`DELETE /api/equipment/{equipmentId}/images/{imageId}`)
- **Headers:** `X-Partner-Id: <ownerPartnerId>`
- **Behavior:** Removes image from collection; maintains single primary image invariant.
- **Response:** `200 OK` with `ApiResponse<EquipmentResponse>`

### 8. Discover Available Equipment (`GET /api/equipment/available`)
- **Authorization:** Public / Farmer
- **Response:** `200 OK` with `ApiResponse<List<EquipmentSummaryResponse>>`

### 9. Paginated Discover Available Equipment (`GET /api/equipment/available/page`)
- **Query Params:** `page` (default 0), `size` (default 20, max 100)
- **Response:** `200 OK` with `ApiResponse<Page<EquipmentSummaryResponse>>`

### 10. Multi-Criteria Search (`GET /api/equipment/search`)
- **Query Params:** `category`, `minPrice`, `maxPrice`, `availabilityStatus`, `locationAddress`
- **Response:** `200 OK` with `ApiResponse<List<EquipmentSummaryResponse>>`

### 11. Paginated Multi-Criteria Search (`GET /api/equipment/search/page`)
- **Query Params:** `category`, `minPrice`, `maxPrice`, `availabilityStatus`, `locationAddress`, `page`, `size`
- **Response:** `200 OK` with `ApiResponse<Page<EquipmentSummaryResponse>>`

### 12. Get Partner Equipment (`GET /api/equipment/partner/{partnerId}`)
- **Headers:** `X-Partner-Id: <partnerId>`
- **Response:** `200 OK` with `ApiResponse<List<EquipmentResponse>>`

---

## 5. Security & Authorization Matrix

| Operation | Farmer | Owner Partner | Non-Owner Partner | Admin / Operator | Unauthenticated |
| --- | --- | --- | --- | --- | --- |
| **Discover & Search** | `ALLOWED` | `ALLOWED` | `ALLOWED` | `ALLOWED` | `ALLOWED` |
| **View Details** | `ALLOWED` | `ALLOWED` | `ALLOWED` | `ALLOWED` | `ALLOWED` |
| **Create Equipment** | `DENIED` | `ALLOWED` | `ALLOWED` | `DENIED` | `DENIED` (`401`) |
| **Update Equipment** | `DENIED` | `ALLOWED` | `DENIED` (`403`) | `DENIED` | `DENIED` (`401`) |
| **Enable / Disable** | `DENIED` | `ALLOWED` | `DENIED` (`403`) | `DENIED` | `DENIED` (`401`) |
| **Delete Equipment** | `DENIED` | `ALLOWED` | `DENIED` (`403`) | `DENIED` | `DENIED` (`401`) |
| **Delete Image** | `DENIED` | `ALLOWED` | `DENIED` (`403`) | `DENIED` | `DENIED` (`401`) |

---

## 6. Test Suite & Verification Summary

### Backend Unit & Slice Test Suite (`.\mvnw.cmd clean verify`):
- **Total Tests Executed:** 39
- **Passed:** 38
- **Skipped:** 1 (`DemoApplicationTests` requiring live DB server)
- **Failures / Errors:** **0**

### Test Breakdown:
1. `EquipmentEntityTest`: Entity domain defaults and relationship methods (3 tests)
2. `EquipmentDtoValidationTest`: Jakarta Validation DTO constraints (5 tests)
3. `EquipmentMapperTest`: Mapper DTO ↔ Entity transformations (4 tests)
4. `EquipmentServiceTest`: Service business logic & partner ownership (11 tests)
5. `EquipmentControllerTest`: MockMvc REST controller slice tests (5 tests)
6. `EquipmentImageManagementTest`: Single primary image invariant & display order (6 tests)
7. `EquipmentPaginationAndLockingTest`: Page size capping & optimistic locking 409 handling (4 tests)

---

## 7. Deferred Integrations & Known Boundaries

1. **Review & Rating Integration:** DEFERRED. Placeholder package `com.agrorental.review` exists; no Java entities or controllers implemented.
2. **Payment Integration:** DEFERRED. Placeholder package `com.agrorental.payment` exists; no payment gateway SDKs or Java code implemented.
3. **External Notifications:** DEFERRED. Native Spring `ApplicationEventPublisher` architecture is recommended for post-commit event listener processing.

---

## 8. Developer Handoff & Change Safety Rules

1. **Layer Integrity:** Never call repositories directly from Mappers or Controllers. Always route through `EquipmentService`.
2. **Entity Exposure:** Never return `@Entity` objects in Controller endpoints. Always use `EquipmentResponse` or `EquipmentSummaryResponse` DTOs.
3. **Monetary Precision:** Always use `BigDecimal` for `rentalPrice` calculations. Never convert to `double` or `float`.
4. **Partner Security:** Never trust `partnerId` in client JSON for authorization. Always validate against authenticated partner context via `EquipmentService.validateOwnership()`.
5. **State Integrity:** Never allow client requests to manually set `availabilityStatus = BOOKED`. Booking status updates must be driven by the Booking lifecycle.
6. **Optimistic Locking:** Retain `@Version` in `BaseEntity` to ensure concurrent edits fail safely with HTTP 409 Conflict.
