#!/usr/bin/env bash
# Black-box checks against a RUNNING backend (default http://localhost:8080).
#   ./scripts/smoke-test.sh [base-url] [admin-email] [admin-password]
# Run it right after the first start: it proves auth, roles and privacy rules behave as designed.
set -u
BASE="${1:-http://localhost:8080}"
ADMIN_EMAIL="${2:-admin@findajobremotely.local}"
ADMIN_PASS="${3:-DevAdmin-2026-Pass1}"
PASS=0; FAIL=0
ok()   { PASS=$((PASS+1)); echo "PASS  $1"; }
bad()  { FAIL=$((FAIL+1)); echo "FAIL  $1  ($2)"; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
expect() { local name="$1" want="$2"; shift 2; local got; got=$(code "$@"); [ "$got" = "$want" ] && ok "$name" || bad "$name" "wanted $want, got $got"; }
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)" 2>/dev/null; }

STAMP=$(date +%s)
CAND="cand$STAMP@example.com"; EMP="emp$STAMP@example.com"; PW="Sunrise-Walk-2026"

expect "health endpoint is up"                       200 "$BASE/actuator/health"
expect "public job list needs no login"              200 "$BASE/api/jobs"
expect "candidate API rejects anonymous"             401 "$BASE/api/candidate/profile"
expect "admin API rejects anonymous"                 401 "$BASE/api/admin/stats"
expect "garbage bearer token rejected"               401 -H "Authorization: Bearer abc.def.ghi" "$BASE/api/candidate/profile"
expect "weak password rejected"                      400 -X POST -H 'Content-Type: application/json' -d "{\"fullName\":\"A B\",\"email\":\"w$STAMP@example.com\",\"password\":\"short\",\"role\":\"CANDIDATE\"}" "$BASE/api/auth/register"
expect "cannot self-register as ADMIN"               400 -X POST -H 'Content-Type: application/json' -d "{\"fullName\":\"A B\",\"email\":\"x$STAMP@example.com\",\"password\":\"$PW\",\"role\":\"ADMIN\"}" "$BASE/api/auth/register"

CAND_TOKEN=$(curl -s -X POST -H 'Content-Type: application/json' -d "{\"fullName\":\"Test Candidate\",\"email\":\"$CAND\",\"password\":\"$PW\",\"role\":\"CANDIDATE\"}" "$BASE/api/auth/register" | json "d['accessToken']")
EMP_TOKEN=$(curl -s -X POST -H 'Content-Type: application/json' -d "{\"fullName\":\"Test Employer\",\"email\":\"$EMP\",\"password\":\"$PW\",\"role\":\"EMPLOYER\",\"companyName\":\"Smoke Co\"}" "$BASE/api/auth/register" | json "d['accessToken']")
[ -n "$CAND_TOKEN" ] && [ -n "$EMP_TOKEN" ] && ok "registered a candidate and an employer" || bad "registration" "no token returned"

expect "duplicate email rejected"                    409 -X POST -H 'Content-Type: application/json' -d "{\"fullName\":\"Test Candidate\",\"email\":\"$CAND\",\"password\":\"$PW\",\"role\":\"CANDIDATE\"}" "$BASE/api/auth/register"
expect "candidate can read own profile"              200 -H "Authorization: Bearer $CAND_TOKEN" "$BASE/api/candidate/profile"
expect "candidate blocked from employer API"         403 -H "Authorization: Bearer $CAND_TOKEN" "$BASE/api/employer/jobs"
expect "candidate blocked from admin API"            403 -H "Authorization: Bearer $CAND_TOKEN" "$BASE/api/admin/stats"
expect "employer blocked from candidate API"         403 -H "Authorization: Bearer $EMP_TOKEN" "$BASE/api/candidate/profile"
expect "employer blocked from admin API"             403 -H "Authorization: Bearer $EMP_TOKEN" "$BASE/api/admin/users"
expect "admin cannot sign in on the public form"     401 -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" "$BASE/api/auth/login"
expect "candidate cannot use the admin sign-in"      401 -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$CAND\",\"password\":\"$PW\"}" "$BASE/api/auth/admin/login"
expect "wrong password rejected"                     401 -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$CAND\",\"password\":\"Wrong-Pass-123\"}" "$BASE/api/auth/login"

# Employer needs a company description before posting
curl -s -o /dev/null -X PUT -H "Authorization: Bearer $EMP_TOKEN" -H 'Content-Type: application/json' -d '{"companyName":"Smoke Co","about":"We test things thoroughly every day."}' "$BASE/api/employer/profile"
JOB_ID=$(curl -s -X POST -H "Authorization: Bearer $EMP_TOKEN" -H 'Content-Type: application/json' -d '{"title":"Smoke Test Engineer","description":"Write and run smoke tests for a remote product.","type":"FULL_TIME","experienceLevel":"JUNIOR","skills":["Bash","curl"],"remoteRegion":"Worldwide","openings":1}' "$BASE/api/employer/jobs" | json "d['id']")
[ -n "$JOB_ID" ] && ok "employer posted a job" || bad "employer posted a job" "no id"
expect "new job is publicly visible"                 200 "$BASE/api/jobs/$JOB_ID"

# Candidate profile + apply
curl -s -o /dev/null -X PUT -H "Authorization: Bearer $CAND_TOKEN" -H 'Content-Type: application/json' -d '{"headline":"Shell scripter","skills":["Bash","curl"],"phone":"+91 90000 00000","expectedMonthlySalary":500,"currency":"USD","publicProfile":true}' "$BASE/api/candidate/profile"
expect "candidate applies"                           201 -X POST -H "Authorization: Bearer $CAND_TOKEN" -H 'Content-Type: application/json' -d '{"coverNote":"hello"}' "$BASE/api/candidate/jobs/$JOB_ID/apply"
expect "double apply rejected"                       409 -X POST -H "Authorization: Bearer $CAND_TOKEN" -H 'Content-Type: application/json' -d '{}' "$BASE/api/candidate/jobs/$JOB_ID/apply"

# Privacy: public talent search must not leak private fields
TALENT=$(curl -s -H "Authorization: Bearer $EMP_TOKEN" "$BASE/api/employer/talent?q=Shell")
echo "$TALENT" | grep -q "Shell scripter" && ok "talent search finds the public profile" || bad "talent search" "profile not found"
echo "$TALENT" | grep -qE "90000|$CAND|expectedMonthlySalary|phone" && bad "talent search hides private fields" "private data present" || ok "talent search hides private fields"

# Applicant view for the employer they applied to DOES include contact details
APPS=$(curl -s -H "Authorization: Bearer $EMP_TOKEN" "$BASE/api/employer/jobs/$JOB_ID/applications")
echo "$APPS" | grep -q "$CAND" && ok "applicant contact details visible to the employer applied to" || bad "applicant view" "email missing"

# Admin
ADMIN_TOKEN=$(curl -s -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" "$BASE/api/auth/admin/login" | json "d['accessToken']")
if [ -n "$ADMIN_TOKEN" ]; then
  ok "admin signed in through the admin portal"
  expect "admin stats"                               200 -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/api/admin/stats"
  ADMIN_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/api/admin/users?q=$CAND")
  echo "$ADMIN_USER" | grep -qE "90000|passwordHash" && bad "admin user list hides private fields" "leak" || ok "admin user list hides private fields"
else
  bad "admin sign-in" "pass your admin email/password as arguments 2 and 3"
fi

# Lockout: 5 bad passwords then even the right one is refused
for i in 1 2 3 4 5; do code -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$EMP\",\"password\":\"Nope-Nope-123\"}" "$BASE/api/auth/login" >/dev/null; done
expect "account locks after repeated failures"       401 -X POST -H 'Content-Type: application/json' -d "{\"email\":\"$EMP\",\"password\":\"$PW\"}" "$BASE/api/auth/login"

echo; echo "$PASS passed, $FAIL failed"; [ "$FAIL" -eq 0 ]
