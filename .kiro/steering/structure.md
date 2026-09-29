# Project Structure

## Root Layout

```
lab-manager/
├── backend/          # Spring Boot Maven project
├── frontend/         # Vite + React TypeScript project
└── .kiro/            # Kiro specs and steering
```

## Backend — `backend/`

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/campuslab/
    │   │   ├── CampusLabApplication.java       # Entry point
    │   │   ├── auth/                           # Authentication domain
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   └── dto/                        # LoginRequest, LoginResponse, UserSummary
    │   │   ├── user/                           # User domain (no controller — internal use only)
    │   │   │   ├── User.java
    │   │   │   ├── UserRole.java               # Enum: ALUNO, PROFESSOR
    │   │   │   └── UserRepository.java
    │   │   ├── laboratory/                     # Laboratory domain
    │   │   │   ├── Laboratory.java
    │   │   │   ├── LaboratoryController.java
    │   │   │   ├── LaboratoryRepository.java
    │   │   │   ├── LaboratoryService.java
    │   │   │   └── dto/                        # LaboratoryRequest, LaboratoryResponse, LaboratoryDetailResponse
    │   │   ├── reservation/                    # Reservation domain
    │   │   │   ├── Reservation.java
    │   │   │   ├── ReservationController.java
    │   │   │   ├── ReservationRepository.java
    │   │   │   ├── ReservationService.java
    │   │   │   └── dto/                        # ReservationRequest, ReservationResponse
    │   │   └── shared/                         # Cross-cutting concerns
    │   │       ├── config/SecurityConfig.java
    │   │       ├── dto/ErrorResponse.java
    │   │       ├── exception/
    │   │       │   ├── ErrorHandler.java       # @RestControllerAdvice
    │   │       │   └── exceptions/             # Domain exception classes
    │   │       └── security/
    │   │           ├── JwtService.java
    │   │           └── SecurityFilter.java     # OncePerRequestFilter
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           └── V1__Create_initial_schema.sql
    └── test/java/com/campuslab/
        ├── auth/
        ├── laboratory/
        ├── reservation/
        ├── security/
        └── shared/
```

## Frontend — `frontend/`

```
frontend/
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── src/
    ├── App.tsx                     # Router setup
    ├── main.tsx
    ├── api/
    │   ├── axiosClient.ts          # Centralized Axios instance + interceptors
    │   ├── authApi.ts
    │   ├── laboratoryApi.ts
    │   └── reservationApi.ts
    ├── components/
    │   ├── ProtectedRoute.tsx      # Redirects to /login if unauthenticated
    │   ├── RoleGuard.tsx           # Conditionally renders based on role
    │   ├── LaboratoryForm.tsx
    │   ├── ReservationForm.tsx
    │   ├── LoadingSpinner.tsx
    │   └── ErrorMessage.tsx
    ├── pages/
    │   ├── LoginPage.tsx
    │   ├── LaboratoryListPage.tsx
    │   ├── LaboratoryDetailPage.tsx
    │   └── NotFoundPage.tsx
    ├── store/                      # Zustand stores (authStore)
    ├── types/                      # TypeScript types: auth.ts, laboratory.ts, reservation.ts, api.ts
    └── test/setup.ts
```

## Naming Conventions

### Backend
- Packages are domain-first: `auth`, `user`, `laboratory`, `reservation`, with `shared` for cross-cutting code
- DTOs use `Request` / `Response` suffixes; detail variants use `DetailResponse`
- Domain exceptions are descriptive: `ResourceNotFoundException`, `ReservationConflictException`, `LaboratoryHasFutureReservationsException`, `DuplicateNameException`, `InvalidReservationPeriodException`, `InvalidCredentialsException`
- All IDs are `UUID`; all timestamps are `Instant` (mapped to `TIMESTAMP WITH TIME ZONE`)

### Frontend
- API modules mirror backend domains: `authApi.ts`, `laboratoryApi.ts`, `reservationApi.ts`
- All API calls go through `axiosClient` (never raw `axios`)
- JWT token stored in `localStorage` under key `campuslab_token`; user info under `campuslab_user`

## Authorization Model

| Endpoint pattern | ALUNO | PROFESSOR |
|---|---|---|
| `POST /api/auth/login` | ✅ public | ✅ public |
| `GET /api/laboratories/**` | ✅ | ✅ |
| `GET /api/reservations/**` | ✅ | ✅ |
| All other `/api/laboratories/**` | ❌ | ✅ |
| All other `/api/reservations/**` | ❌ | ✅ |
