# AR Denial Analytics

A full-stack assessment app for reviewing fictional insurance claims and denial patterns. The request flow is MySQL → Spring Boot REST API → React dashboard. No patient information is included.

## Implemented features

- Payer denied-claim amount and count summaries, sorted by billed amount.
- Denial-reason amount summary.
- Filterable, zero-based paginated denied claims with inclusive date filters.
- Consistent JSON validation and server error responses.
- Dashboard charts, summaries, filters, pagination, and visible loading, empty, and error states.
- Repeatable schema and sample data through Flyway.

The “denied amount” metrics sum `billed_amount` for claims whose current status is `denied`. There is no separate denied amount field, and these figures are not confirmed unrecoverable financial losses.

## Technology

- Java 21, Spring Boot, Maven, Spring Web, Spring Data JPA, Bean Validation, MySQL Connector/J, Flyway
- MySQL 8.4
- React, Vite, JavaScript, Recharts, plain CSS
- Docker Compose

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- Docker Compose v2, or a local MySQL 8.4 server

## Project layout

```text
backend/   Spring Boot API, Flyway migrations, and tests
frontend/  React/Vite dashboard
docker-compose.yml
README.md
```

## Run the application locally

Open three terminals at the repository root. Start the database first, then run the backend and frontend in their own terminals. Start only one backend instance at a time because the default API port is 8080.

### 1. Start MySQL with Docker Compose

From the repository root:

```powershell
docker compose up -d
```

Compose creates database `ar_denial_analytics`, app user `ar_app`, and persists data in the `mysql_data` volume. It listens on port 3306. The local-only development defaults are `ar_app` / `local_dev_password`; the default root password is `local_root_password`. These values are for local assessment use only. If another MySQL service already occupies port 3306, stop that service before starting Compose or use an available MySQL instance and set the backend variables below.

### 2. Start the backend

With MySQL running, in a second terminal:

```powershell
cd backend
mvn spring-boot:run
```

The backend defaults to `http://localhost:8080`. On this assessment computer, the configured local MySQL instance is on port 3307. To use it instead of Compose, run these commands in the backend terminal before starting Maven:

```powershell
$env:DB_URL="jdbc:mysql://127.0.0.1:3307/ar_denial_analytics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="ar_app"
$env:DB_PASSWORD="local_dev_password"
mvn spring-boot:run
```

Use either backend start command, not both. Flyway applies the migrations on first startup, and Hibernate validates the resulting schema.

### 3. Start the frontend

In a third terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal, usually `http://localhost:5173`. The frontend defaults to the API at `http://localhost:8080/api`. To use a different API URL, copy `frontend/.env.example` to `frontend/.env.local`, set `VITE_API_BASE_URL`, and restart Vite. The backend allows the Vite origin configured by `FRONTEND_ORIGIN` (default `http://localhost:5173`).

To stop each process, press **Ctrl+C** in its terminal. If the backend reports that port 8080 is already in use, stop the other backend instance before starting it again.

### Database migrations and sample data

On the first backend startup, Flyway applies `V1__create_claims_table.sql`, `V2__seed_sample_claims.sql`, and `V3__rebalance_sample_claims.sql` in order. V1 creates the constrained `claims` table and its index, V2 inserts 75 deterministic fictional records, and V3 distributes data across recurring payer and denial-reason groups. Paid records have a null denial reason; some paid records also have a denial date, while records never denied have a null denial date. The migrations run once per database. To recreate the sample database, create a new empty database and point the backend at it; Flyway will apply all three migrations. Do not rerun seed SQL against an already migrated database.

## Configuration

Backend reads these environment variables (shown defaults are for local development):

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ar_denial_analytics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `DB_USERNAME` | `ar_app` | Database user |
| `DB_PASSWORD` | `local_dev_password` | Database password |
| `SERVER_PORT` | `8080` | API port |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Allowed browser origin |
| `DB_NAME` | `ar_denial_analytics` | Compose database name |
| `MYSQL_ROOT_PASSWORD` | `local_root_password` | Compose root password |

Frontend defaults to `http://localhost:8080/api`. To change it, copy `frontend/.env.example` to `frontend/.env.local`, set `VITE_API_BASE_URL` (for example `http://localhost:8080/api`), and restart Vite.

## Database index

The composite index `idx_claims_status_denied_at` on `(status, denied_at)` matches the common denied-claims query shape: status equality followed by an optional denied-date range. It can help the database evaluate that pattern, depending on the query plan and data distribution. It does not guarantee faster performance for every query or dataset.

## API examples

Payer summary:

```http
GET http://localhost:8080/api/analytics/payers
```

Denial reason summary:

```http
GET http://localhost:8080/api/analytics/reasons
```

Denied claims, filtered and paginated (dates are inclusive; page is zero based; default size is 10 and maximum size is 100):

```http
GET http://localhost:8080/api/claims?payer=Summit%20Mutual&reason=Authorization%20missing&startDate=2025-01-01&endDate=2025-12-31&page=0&size=10
```

The claims response includes `content`, `page`, `size`, `totalElements`, and `totalPages`. Invalid input returns an HTTP error status and JSON error body. For example:

```http
GET http://localhost:8080/api/claims?startDate=2025-12-31&endDate=2025-01-01
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_DATE_RANGE","message":"startDate must be on or before endDate"}
```

A basic process check is available at `GET http://localhost:8080/api/health`.

## Tests and production builds

From `backend/`:

```sh
mvn test
mvn package
```

From `frontend/`:

```sh
npm run build
```

The backend API integration tests use small deterministic H2 test data; normal application runs use MySQL. `mvn package` also runs the backend tests before creating the executable JAR.

## Known limitations

- No authentication or authorization; this is an assessment app.
- Denial analytics are based on current claim status and billed amount. The schema cannot establish final financial loss or recovery.
- Analytics cover the complete sample dataset; the claims list is filterable and paginated.
- Sample data is fictional and exists only to support local evaluation.


