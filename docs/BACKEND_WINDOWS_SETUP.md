# Gabarita+ Backend on Windows + VS Code + Supabase

This runbook is written for a first local setup on Windows.

## Target

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/api/swagger-ui.html`

## Estimated time

1. Install Java 21: 10 to 15 minutes
2. Install Maven: 5 to 10 minutes
3. Install Docker Desktop: 10 to 20 minutes
4. Get Supabase credentials: 5 to 10 minutes
5. Create backend `.env`: 5 minutes
6. Check prerequisites: 2 minutes
7. Run backend: 5 to 10 minutes
8. Test Swagger + JWT + frontend integration: 10 to 15 minutes

Total: about 50 to 85 minutes

## 1. Install Java 21

Download the official Temurin 21 JDK MSI:

- https://adoptium.net/installation/windows/

During installation:

- click `Next`
- keep `Set JAVA_HOME variable`
- keep `Add to PATH`
- click `Install`

Validate:

```powershell
java -version
where java
```

Expected: Java 21

## 2. Install Maven

Official guide:

- https://maven.apache.org/install

Steps:

1. Download the latest `Binary zip archive`
2. Extract to:

```text
C:\Tools\apache-maven-3.9.x
```

3. Add system variable:

```text
MAVEN_HOME=C:\Tools\apache-maven-3.9.x
```

4. Add to system `Path`:

```text
C:\Tools\apache-maven-3.9.x\bin
```

Validate:

```powershell
mvn -version
where mvn
```

Expected: Maven found and using Java 21

## 3. Install Docker Desktop

Official guide:

- https://docs.docker.com/desktop/setup/install/windows-install/

If WSL 2 is not installed yet:

```powershell
wsl --install
```

Validate:

```powershell
docker --version
docker compose version
wsl --version
```

Note: Docker is recommended, but not required for the first successful run with Supabase.

## 4. Install Java support in VS Code

Install this official extension pack:

- `Extension Pack for Java`

Official docs:

- https://code.visualstudio.com/docs/java/java-tutorial

## 5. Get Supabase credentials

Open your Supabase project:

1. click `Connect`
2. choose `Session pooler`
3. copy the host, port, database, user and password

Official docs:

- https://supabase.com/docs/guides/database/connecting-to-postgres

If needed, reset your database password in the Supabase dashboard.

## 6. Create `.env`

In `backend/`:

```powershell
cd backend
Copy-Item .env.supabase.example .env
notepad .env
```

Fill the values from Supabase.

Important:

- keep `?sslmode=require` in `DB_URL`
- keep `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173`
- set a long `JWT_SECRET` with at least 32 characters
- keep `SEED_ENABLED=true` only while you need demo users and sample questions

## 7. Check prerequisites

Run:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\check-prereqs.ps1
```

This checks:

- Java
- Maven
- Docker
- `.env`
- port 8080
- DB host reachability

## 8. Run backend

Run:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

This script:

- loads `.env`
- exposes env vars to the current process
- runs `mvn clean spring-boot:run`

## 9. Validate startup

Open these URLs:

- `http://localhost:8080/api/actuator/health`
- `http://localhost:8080/api/swagger-ui.html`
- `http://localhost:8080/api/v3/api-docs`

## 10. Validate JWT and protected endpoints

Use the built-in test script:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\test-api.ps1
```

Default credentials used:

- `user@gabaritaplus.com`
- `User@123`

The script validates:

- health
- login
- dashboard
- profile
- questions
- refresh token

## 11. Validate frontend integration

Make sure the frontend is running on `http://localhost:3000`.

Then:

1. open `http://localhost:3000/login`
2. log in with:

```text
user@gabaritaplus.com
User@123
```

3. verify:

- dashboard loads
- questions load
- profile loads
- mock exams load
- no CORS error in browser DevTools

## Common errors

### Java version mismatch

Run:

```powershell
java -version
mvn -version
```

Both must point to Java 21.

### Maven not found

Run:

```powershell
where mvn
echo $env:MAVEN_HOME
```

If Maven is missing, fix `Path` and reopen VS Code.

### Port 8080 already in use

Run:

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Or change `SERVER_PORT` in `.env`.

### PostgreSQL connection error

Check:

- Supabase host
- Supabase username
- Supabase password
- `sslmode=require`
- `Session pooler`

### Invalid JWT

Most common causes:

- `JWT_SECRET` changed after login
- expired token
- stale token in browser storage/cookies

Fix:

- keep one stable `JWT_SECRET`
- restart backend
- log in again

### CORS

Make sure `.env` contains:

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

Restart backend after changing it.

### Flyway migration failed

Usually caused by:

- existing conflicting tables
- partially migrated database
- wrong database target

Best practice:

- use a clean Supabase project for the first run
- do not create the tables manually before Flyway
