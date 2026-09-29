# Tech Stack

## Backend

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2
  - `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`
- **Security**: Spring Security — stateless JWT via JJWT 0.12.6
- **Persistence**: Spring Data JPA + Hibernate; PostgreSQL driver
- **Migrations**: Flyway (`db/migration/V*.sql`); JPA `ddl-auto: validate` (Flyway owns the schema)
- **Build**: Maven (`pom.xml`); parent `spring-boot-starter-parent`
- **Testing**: JUnit 5, jqwik 1.8.5 (property-based), jqwik-spring 0.12.0, Testcontainers 1.20.1 (PostgreSQL)

## Frontend

- **Language**: TypeScript 5.7
- **Framework**: React 18.3 (functional components + hooks)
- **Bundler**: Vite 6
- **Routing**: react-router-dom 6.28
- **HTTP**: Axios 1.7.9 (centralized client in `src/api/axiosClient.ts`)
- **State**: Zustand 5.0.3 (auth store)
- **Styling**: Tailwind CSS 3.4 + PostCSS + Autoprefixer
- **Testing**: Vitest 2.1, @testing-library/react 16.1, fast-check 3.23 + @fast-check/vitest (property-based), jsdom 25

## Database

- PostgreSQL — UUID primary keys, `TIMESTAMP WITH TIME ZONE` for all time columns
- Schema managed exclusively by Flyway; never alter tables manually

## Environment Variables (backend)

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/campuslab` | DB connection |
| `DATABASE_USERNAME` | `postgres` | DB user |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | (base64 key) | HMAC secret |
| `JWT_EXPIRES_IN` | `86400` | Token TTL in seconds |
| `SERVER_PORT` | `8080` | HTTP port |

Frontend uses `VITE_API_BASE_URL` (defaults to `http://localhost:8080`).

## Common Commands

### Backend
```bash
# Run (from backend/)
./mvnw spring-boot:run

# Build JAR
./mvnw package -DskipTests

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=LaboratoryServiceTest
```

### Frontend
```bash
# Install dependencies (from frontend/)
npm install

# Development server
npm run dev

# Production build
npm run build

# Run tests (single pass)
npm run test:run

# Run tests in watch mode
npm test

# Lint
npm run lint
```
