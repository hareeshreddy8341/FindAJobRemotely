# FindAJobRemotely

A marketplace for **remote jobs and internships at startups**. Candidates use it free and owe a small
commission **only after a company records their hire**. Startups post jobs, search candidate profiles and
posts, and see a fit score for every applicant. Administrators have a separate portal.

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Redux Toolkit, React Router, Tailwind CSS 3, Mantine 8 |
| Backend | Spring Boot 3.5 (Java 21), Spring Security, JWT (jjwt), Bean Validation |
| Database | MongoDB 7 (Spring Data MongoDB) |

## What was verified, and what was not

Read this first. It is an honest account of this MVP's status.

| Part | Status |
|---|---|
| React app | **Built and tested in a real browser** (37 end-to-end checks: sign-up, profile, matching, apply, hire, commission, privacy boundaries, admin portal, mobile layout, no console errors). The tests ran against `scripts/mock-server.mjs`, a stand-in API with the same contract. |
| Core backend logic (`core/`: matching, password policy, AES-GCM encryption, application state machine, commission maths, sanitising) | **Compiled and unit-tested** (23 JUnit tests, all pass). |
| Spring Boot layer (security config, controllers, services, Mongo queries) | **Written and syntax-checked, but never compiled or run.** The build environment could not download Maven dependencies, so there was no Spring on the classpath and no MongoDB. Expect to fix a few small issues on the first `mvn package`. |

Run `scripts/smoke-test.sh` against the real backend on first start. It checks authentication, role
separation, lockout and privacy rules end to end (the same script was run against the mock to check the
script itself).

Pinned versions (Spring Boot 3.5.6, jjwt 0.12.6) could not be checked against Maven Central. If Maven can't
resolve them, bump them to the latest 3.5.x and 0.12.x.

## Run it

### Option A: everything in Docker

```bash
cp .env.example .env          # then fill in the secrets (commands are in the file)
docker compose up --build     # app on http://localhost
```
For plain-http local testing set `COOKIE_SECURE=false` in `.env`. In production serve over HTTPS and keep it `true`.

### Option B: development

```bash
# 1. MongoDB
docker run -d -p 27017:27017 --name fajr-mongo mongo:7

# 2. Backend (dev profile supplies throwaway secrets and an admin: admin@findajobremotely.local / DevAdmin-2026-Pass1)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend (proxies /api to :8080)
cd frontend && npm install && npm run dev      # http://localhost:5173
```
Admin portal: `/admin/login`. Backend checks: `./scripts/smoke-test.sh`.

### Option C: frontend only (no Java, no database)

```bash
node scripts/mock-server.mjs            # in-memory API on :8080
cd frontend && npm install && npm run dev
```
Demo logins: `maya@demo.test` / `Candidate-2026-pw`, `hiring@acme.test` / `Employer-2026-pw`,
admin (at `/admin/login`) `admin@demo.test` / `DemoAdmin-2026`. The mock is for development only.

## How it works

**Roles and navigation**
- **Candidate:** Find jobs (best matches and search), Applied jobs, My posts, Profile.
- **Employer:** Post a job, My jobs (applicants, best-fit candidates), Candidate posts, Find talent, Company profile.
- **Admin** (separate portal and sign-in): overview, users (suspend, reactivate, delete, verify companies), jobs and posts moderation, commissions, audit log.

**Matching** (`core/MatchingEngine`): an explainable 0-100 score: skills overlap 55, job-type preference 15,
experience fit 15, role keywords 10, pay expectation 5. Missing data scores half marks. Used to rank jobs for
candidates and candidates for jobs, and shown with matched and missing skills.

**Business model:** when an employer moves an application to *Hired* they enter the agreed monthly pay. A
`Commission` (default 10% of that pay, `COMMISSION_PERCENT`) is created and shown to the candidate. Admins
mark it paid or waived. Collecting the money is manual in this MVP.

## Security design

| Concern | What the code does |
|---|---|
| Passwords | BCrypt (cost 12), 10-64 characters, letter and number required, common and email-based passwords refused |
| Sessions | 15-minute JWT access token held **in memory only**; 7-day refresh token in an `HttpOnly`, `SameSite=Strict`, `Secure` cookie, stored hashed, **rotated on every use** with reuse detection (a replayed token revokes the whole session family) |
| Brute force | Account lock after 5 failed logins (15 min); equal-cost responses for unknown accounts; per-IP rate limits (stricter on login and register) |
| Authorization | Roles enforced at the URL level and the role is re-read from the database on every request, so suspensions and deletions take effect immediately. Admin accounts can only be created from environment variables, never through the API, and sign in only on the admin endpoint |
| Private data | Phone, expected salary and resumes are encrypted at rest with AES-256-GCM, bound to the owner's id so a copied value will not decrypt for someone else. Public views never include them. Employers get contact details and resume only for candidates who **applied to them**. Admins cannot see them at all |
| Input | Bean Validation on every request, control characters stripped, regex input escaped, only `http(s)` links accepted, PDF uploads checked by content (not file name), 2 MB limit, React escapes all output |
| Transport and headers | CORS allow-list, CSP, `X-Frame-Options`, `nosniff`, no-referrer; nginx config adds the same for the app |
| Errors and audit | Generic error messages with no stack traces; audit log for sign-ins, lockouts, admin actions, hires and resume downloads |

No software can honestly promise "no one can hack it". This is a strong baseline. Before real users, add the
items below and get an independent security review.

## Before going to production

- Email verification and password reset (not included).
- CAPTCHA or bot protection on registration and login.
- Move `DATA_ENCRYPTION_KEY` and `JWT_SECRET` to a secrets manager or KMS; back up the encryption key (losing it makes encrypted data unreadable).
- A shared rate-limit store (for example Redis) if you run more than one backend instance; the built-in limiter is per instance.
- Antivirus scanning for uploaded resumes.
- HTTPS in front (enable the HSTS line in `deploy/nginx.conf`), MongoDB backups, dependency scanning.
- A payment flow for commissions, and terms of service and a privacy policy reviewed for your jurisdiction (including India's DPDP Act).

## Project layout

```
backend/   Spring Boot API  (core/ pure logic, model/, repository/, service/, controller/, security/, config/)
frontend/  React app        (pages/, components/, store/ Redux, api/ axios client)
deploy/    nginx.conf
scripts/   mock-server.mjs (dev API), smoke-test.sh (backend checks)
```

## API summary

`POST /api/auth/{register,login,refresh,logout,change-password}`, `POST /api/auth/admin/login`, `GET /api/auth/me`, `DELETE /api/auth/account`
`GET /api/jobs`, `GET /api/jobs/{id}` (public)
`/api/candidate/**` profile, resume, recommendations, apply, applications, commissions, posts
`/api/employer/**` profile, jobs, applications, talent, posts, matches, applicant resume
`/api/admin/**` stats, users, employers/verify, jobs, posts, commissions, audit
