// Development-only mock of the FindAJobRemotely REST API (in memory, no database).
// It follows the same contract as the Spring Boot backend so the React app can be tried without Java or MongoDB.
//   node scripts/mock-server.mjs          -> http://localhost:8080
// Demo accounts: admin@demo.test / DemoAdmin-2026 (admin portal at /admin/login)
// NOT for production: passwords are compared in plain text here.
import http from 'node:http';
import crypto from 'node:crypto';

const PORT = Number(process.env.PORT || 8080);
const PERCENT = 10;
const id = () => crypto.randomBytes(12).toString('hex');
const now = () => new Date().toISOString();

const db = { users: [], cand: {}, emp: {}, jobs: [], apps: [], posts: [], commissions: [], audit: [], tokens: {}, resumes: {} };
const sessions = {}; // access token -> user id

function addUser(role, email, fullName, password) {
  const u = { id: id(), role, email, fullName, password, status: 'ACTIVE', createdAt: now(), lastLoginAt: null };
  db.users.push(u);
  return u;
}
const log = (actor, action, targetType, targetId, details) =>
  db.audit.unshift({ id: id(), actorEmail: actor?.email ?? null, action, targetType, targetId, details, ip: '127.0.0.1', createdAt: now() });

// ---------- seed ----------
addUser('ADMIN', 'admin@demo.test', 'Platform Admin', 'DemoAdmin-2026');
const emp = addUser('EMPLOYER', 'hiring@acme.test', 'Asha Rao', 'Employer-2026-pw');
db.emp[emp.id] = { userId: emp.id, companyName: 'Acme Robotics', website: 'https://acme.example', industry: 'Robotics', companySize: '11-50', about: 'We build warehouse robots and need great remote engineers.', country: 'India', foundedYear: 2021, verified: true, contactPhone: '+91 90000 11111' };
const c1 = addUser('CANDIDATE', 'maya@demo.test', 'Maya Okafor', 'Candidate-2026-pw');
db.cand[c1.id] = { userId: c1.id, headline: 'Frontend developer', about: 'I build accessible React apps.', skills: ['React', 'TypeScript', 'CSS'], experienceLevel: 'JUNIOR', yearsOfExperience: 2, preferredJobTypes: ['FULL_TIME', 'INTERNSHIP'], preferredRoles: ['Frontend Developer'], education: 'B.Tech CSE', country: 'Nigeria', timezone: 'UTC+1', githubUrl: 'https://github.com/maya', linkedinUrl: '', portfolioUrl: '', publicProfile: true, accountActive: true, phone: '+234 800 000 0000', expectedMonthlySalary: 1500, currency: 'USD' };
const c2 = addUser('CANDIDATE', 'ravi@demo.test', 'Ravi Kumar', 'Candidate-2026-pw');
db.cand[c2.id] = { userId: c2.id, headline: 'Data science student', about: 'Final-year B.Tech, ML and analytics.', skills: ['Python', 'Pandas', 'SQL'], experienceLevel: 'FRESHER', yearsOfExperience: 0, preferredJobTypes: ['INTERNSHIP'], preferredRoles: ['Data Analyst'], education: 'B.Tech AI & DS', country: 'India', timezone: 'UTC+5:30', githubUrl: '', linkedinUrl: '', portfolioUrl: '', publicProfile: true, accountActive: true, phone: '+91 98888 77777', expectedMonthlySalary: 300, currency: 'USD' };
const mkJob = (t, d, type, lvl, skills, min, max, cur, region, open) => db.jobs.push({ id: id(), employerId: emp.id, companyName: 'Acme Robotics', title: t, description: d, type, experienceLevel: lvl, skills, salaryMin: min, salaryMax: max, currency: cur, remoteRegion: region, openings: open, hiredCount: 0, status: 'OPEN', createdAt: now(), updatedAt: now() });
mkJob('Frontend Developer', 'Build the operator dashboard for our robot fleet.\nReact, TypeScript and a love for accessible interfaces.', 'FULL_TIME', 'JUNIOR', ['React', 'TypeScript', 'CSS'], 1200, 2000, 'USD', 'Worldwide', 2);
mkJob('Data Analyst Intern', 'Help us understand fleet telemetry. You will write SQL and Python every day.', 'INTERNSHIP', 'FRESHER', ['Python', 'SQL', 'Pandas'], 200, 400, 'USD', 'India and nearby time zones', 3);
mkJob('Senior Backend Engineer', 'Own our event pipeline. Java, Kafka and a calm approach to on-call.', 'CONTRACT', 'SENIOR', ['Java', 'Kafka', 'PostgreSQL'], 4000, 6000, 'USD', 'Europe and Africa', 1);
db.posts.push({ id: id(), authorId: c1.id, type: 'PROJECT', title: 'Accessible date picker', content: 'I built a keyboard-first date picker with full screen reader support and published it on npm.', tags: ['React', 'a11y'], status: 'VISIBLE', createdAt: now(), updatedAt: now() });

// ---------- helpers ----------
const canon = (s) => String(s || '').trim().toLowerCase();
const userOf = (uid) => db.users.find((u) => u.id === uid);
const candComplete = (p) => p.headline && p.skills.length > 0;
const empComplete = (p) => p.companyName && p.about;
const pub = (p) => ({ userId: p.userId, fullName: userOf(p.userId)?.fullName ?? 'Unknown', headline: p.headline, about: p.about, skills: p.skills, experienceLevel: p.experienceLevel, yearsOfExperience: p.yearsOfExperience, preferredJobTypes: p.preferredJobTypes, preferredRoles: p.preferredRoles, education: p.education, country: p.country, timezone: p.timezone, githubUrl: p.githubUrl, linkedinUrl: p.linkedinUrl, portfolioUrl: p.portfolioUrl });
const userView = (u) => ({ id: u.id, email: u.email, fullName: u.fullName, role: u.role, status: u.status, createdAt: u.createdAt, lastLoginAt: u.lastLoginAt });
const jobView = (j, counts) => ({ ...j, companyVerified: !!db.emp[j.employerId]?.verified, applicantCount: counts ? db.apps.filter((a) => a.jobId === j.id).length : undefined });
const postView = (p) => { const a = userOf(p.authorId); const c = db.cand[p.authorId]; return { ...p, authorName: a?.fullName ?? 'Unknown', authorHeadline: c?.headline }; };
const page = (list, q) => { const size = Math.max(1, Math.min(+q.get('size') || 10, 50)); const pg = Math.max(0, +q.get('page') || 0); return { content: list.slice(pg * size, pg * size + size), page: pg, size, totalElements: list.length, totalPages: Math.ceil(list.length / size) }; };
const levelIdx = { FRESHER: 0, JUNIOR: 1, MID: 2, SENIOR: 3 };
function score(p, j) {
  const cs = new Set(p.skills.map(canon)); const js = j.skills.map(canon);
  const matched = js.filter((s) => cs.has(s)); const missing = js.filter((s) => !cs.has(s));
  const skill = js.length ? matched.length / js.length : 0.5;
  const type = p.preferredJobTypes.length ? (p.preferredJobTypes.includes(j.type) ? 1 : 0) : 0.5;
  const exp = j.type === 'INTERNSHIP' && p.experienceLevel === 'FRESHER' ? 1 : !p.experienceLevel ? 0.5 : [1, 0.5, 0][Math.min(2, Math.abs(levelIdx[p.experienceLevel] - levelIdx[j.experienceLevel]))];
  const toks = (p.preferredRoles[0] || '').toLowerCase().split(/[^a-z0-9+#.]+/).filter((t) => t.length >= 3);
  const role = !toks.length ? 0.5 : toks.filter((t) => j.title.toLowerCase().includes(t)).length / toks.length;
  const pay = !p.expectedMonthlySalary || !j.salaryMax || p.currency !== j.currency ? 0.5 : p.expectedMonthlySalary <= j.salaryMax ? 1 : 0;
  return { score: Math.round(55 * skill + 15 * type + 15 * exp + 10 * role + 5 * pay), matchedSkills: matched, missingSkills: missing };
}

// ---------- server ----------
const send = (res, status, body, headers = {}) => { res.writeHead(status, { 'Content-Type': 'application/json', ...headers }); res.end(body === undefined ? '' : JSON.stringify(body)); };
const err = (res, status, message) => send(res, status, { status, message });
const readBody = (req) => new Promise((resolve) => { const chunks = []; req.on('data', (c) => chunks.push(c)); req.on('end', () => resolve(Buffer.concat(chunks))); });
const cookieHeader = (v, max) => `fajr_refresh=${v}; Path=/api/auth; HttpOnly; SameSite=Strict; Max-Age=${max}`;

function session(u, res, status = 200) {
  const access = crypto.randomBytes(24).toString('hex'); sessions[access] = u.id;
  const refresh = crypto.randomBytes(24).toString('hex'); db.tokens[refresh] = u.id;
  send(res, status, { accessToken: access, expiresInSeconds: 900, user: userView(u) }, { 'Set-Cookie': cookieHeader(refresh, 604800) });
}

http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://x'); const path = url.pathname; const q = url.searchParams; const m = req.method;
    const raw = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(m) ? await readBody(req) : null;
    const ctype = req.headers['content-type'] || '';
    const body = raw && raw.length && ctype.includes('json') ? JSON.parse(raw.toString() || '{}') : {};
    const bearer = (req.headers.authorization || '').replace('Bearer ', '');
    const me = sessions[bearer] ? userOf(sessions[bearer]) : null;
    const active = me && me.status === 'ACTIVE' ? me : null;
    const cookies = Object.fromEntries((req.headers.cookie || '').split(';').map((c) => c.trim().split('=')));
    let r;

    // ---- auth ----
    if (m === 'POST' && path === '/api/auth/register') {
      if (db.users.some((u) => u.email === canon(body.email))) return err(res, 409, 'An account with this email already exists');
      if (!['CANDIDATE', 'EMPLOYER'].includes(body.role)) return err(res, 400, 'Choose either a candidate or an employer account');
      if (!body.password || body.password.length < 10 || !/[A-Za-z]/.test(body.password) || !/\d/.test(body.password)) return err(res, 400, 'Password must be at least 10 characters with a letter and a number');
      const u = addUser(body.role, canon(body.email), body.fullName.trim(), body.password);
      if (u.role === 'CANDIDATE') db.cand[u.id] = { userId: u.id, headline: null, about: null, skills: [], experienceLevel: null, yearsOfExperience: null, preferredJobTypes: [], preferredRoles: [], education: null, country: null, timezone: null, githubUrl: null, linkedinUrl: null, portfolioUrl: null, publicProfile: true, accountActive: true, phone: null, expectedMonthlySalary: null, currency: null };
      else db.emp[u.id] = { userId: u.id, companyName: body.companyName, website: null, industry: null, companySize: null, about: null, country: null, foundedYear: null, verified: false, contactPhone: null };
      log(u, 'REGISTER', 'USER', u.id, `role=${u.role}`);
      return session(u, res, 201);
    }
    if (m === 'POST' && (path === '/api/auth/login' || path === '/api/auth/admin/login')) {
      const u = db.users.find((x) => x.email === canon(body.email));
      const adminPortal = path.endsWith('/admin/login');
      if (!u || u.password !== body.password || (u.role === 'ADMIN') !== adminPortal) return err(res, 401, 'Invalid email or password, or the account is temporarily locked');
      if (u.status !== 'ACTIVE') return err(res, 403, 'This account has been suspended. Please contact support.');
      u.lastLoginAt = now(); if (u.role === 'ADMIN') log(u, 'ADMIN_LOGIN', 'USER', u.id, '');
      return session(u, res);
    }
    if (m === 'POST' && path === '/api/auth/refresh') {
      const uid = db.tokens[cookies.fajr_refresh]; const u = uid && userOf(uid);
      if (!u || u.status !== 'ACTIVE') return err(res, 401, 'Session expired. Please sign in again.');
      delete db.tokens[cookies.fajr_refresh]; return session(u, res);
    }
    if (m === 'POST' && path === '/api/auth/logout') { delete db.tokens[cookies.fajr_refresh]; return send(res, 200, { message: 'Signed out' }, { 'Set-Cookie': cookieHeader('', 0) }); }
    if (path === '/api/jobs' && m === 'GET') {
      let list = db.jobs.filter((j) => j.status === 'OPEN');
      const t = canon(q.get('q')); if (t) list = list.filter((j) => [j.title, j.companyName, ...j.skills].some((x) => canon(x).includes(t)));
      if (q.get('type')) list = list.filter((j) => j.type === q.get('type'));
      if (q.get('level')) list = list.filter((j) => j.experienceLevel === q.get('level'));
      if (q.get('skill')) list = list.filter((j) => j.skills.some((s) => canon(s) === canon(q.get('skill'))));
      list = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
      const p = page(list, q); return send(res, 200, { ...p, content: p.content.map((j) => jobView(j, false)) });
    }
    let mm;
    if ((mm = path.match(/^\/api\/jobs\/([^/]+)$/)) && m === 'GET') {
      const j = db.jobs.find((x) => x.id === mm[1]); const owner = active && j && active.id === j.employerId;
      if (!j || (j.status !== 'OPEN' && !owner && active?.role !== 'ADMIN')) return err(res, 404, 'Job not found');
      return send(res, 200, jobView(j, owner || active?.role === 'ADMIN'));
    }

    // ---- everything below needs a signed-in user with the right role ----
    if (!path.startsWith('/api/')) return err(res, 404, 'Not found');
    if (!active) return err(res, 401, 'Authentication required');
    const need = path.startsWith('/api/admin/') ? 'ADMIN' : path.startsWith('/api/employer/') ? 'EMPLOYER' : path.startsWith('/api/candidate/') ? 'CANDIDATE' : null;
    if (need && active.role !== need) return err(res, 403, 'You do not have permission to do this');

    if (path === '/api/auth/me') return send(res, 200, userView(active));
    if (m === 'POST' && path === '/api/auth/change-password') {
      if (active.password !== body.currentPassword) return err(res, 400, 'Your current password is incorrect');
      active.password = body.newPassword; return session(active, res);
    }
    if (m === 'DELETE' && path === '/api/auth/account') {
      if (active.password !== body.password) return err(res, 400, 'Your password is incorrect');
      db.users = db.users.filter((u) => u.id !== active.id); delete db.cand[active.id]; delete db.emp[active.id];
      db.apps = db.apps.filter((a) => a.candidateId !== active.id && a.employerId !== active.id); db.posts = db.posts.filter((p) => p.authorId !== active.id);
      return send(res, 200, { message: 'Your account and data have been deleted' }, { 'Set-Cookie': cookieHeader('', 0) });
    }

    // ---- candidate ----
    if (need === 'CANDIDATE') {
      const p = db.cand[active.id];
      const owner = () => ({ ...p, fullName: active.fullName, email: active.email, hasResume: !!db.resumes[active.id], resumeFileName: db.resumes[active.id]?.name, resumeSizeBytes: db.resumes[active.id]?.size, profileComplete: !!candComplete(p) });
      if (path === '/api/candidate/profile' && m === 'GET') return send(res, 200, owner());
      if (path === '/api/candidate/profile' && m === 'PUT') {
        if (body.fullName) active.fullName = body.fullName.trim();
        Object.assign(p, { headline: body.headline || null, about: body.about || null, skills: body.skills || [], experienceLevel: body.experienceLevel || null, yearsOfExperience: body.yearsOfExperience ?? null, preferredJobTypes: body.preferredJobTypes || [], preferredRoles: body.preferredRoles || [], education: body.education || null, country: body.country || null, timezone: body.timezone || null, githubUrl: body.githubUrl || null, linkedinUrl: body.linkedinUrl || null, portfolioUrl: body.portfolioUrl || null, publicProfile: body.publicProfile !== false, phone: body.phone || null, expectedMonthlySalary: body.expectedMonthlySalary ?? null, currency: body.currency || null });
        return send(res, 200, owner());
      }
      if (path === '/api/candidate/resume' && m === 'POST') {
        const head = raw.slice(0, 2000).toString('latin1'); const fn = /filename="([^"]*)"/.exec(head)?.[1] || 'resume.pdf';
        if (!raw.includes('%PDF-')) return err(res, 400, 'Only PDF files are accepted');
        db.resumes[active.id] = { name: fn.replace(/[^A-Za-z0-9._ -]/g, '_'), size: raw.length, data: raw }; return send(res, 200, { message: 'Resume uploaded' });
      }
      if (path === '/api/candidate/resume' && m === 'DELETE') { delete db.resumes[active.id]; return send(res, 200, { message: 'Resume removed' }); }
      if (path === '/api/candidate/recommendations') {
        const applied = new Set(db.apps.filter((a) => a.candidateId === active.id).map((a) => a.jobId));
        const out = db.jobs.filter((j) => j.status === 'OPEN' && !applied.has(j.id)).map((j) => ({ job: jobView(j, false), ...score(p, j) })).sort((a, b) => b.score - a.score);
        return send(res, 200, out);
      }
      if ((mm = path.match(/^\/api\/candidate\/jobs\/([^/]+)\/apply$/)) && m === 'POST') {
        const j = db.jobs.find((x) => x.id === mm[1] && x.status === 'OPEN');
        if (!j) return err(res, 404, 'This job is no longer open');
        if (!candComplete(p)) return err(res, 400, 'Add a headline and at least one skill to your profile before applying');
        if (db.apps.some((a) => a.jobId === j.id && a.candidateId === active.id)) return err(res, 409, 'You have already applied to this job');
        const a = { id: id(), jobId: j.id, candidateId: active.id, employerId: j.employerId, jobTitle: j.title, companyName: j.companyName, status: 'APPLIED', coverNote: body.coverNote || null, matchScore: score(p, j).score, agreedMonthlySalary: null, appliedAt: now(), updatedAt: now() };
        db.apps.push(a); return send(res, 201, { ...a, commission: null });
      }
      if (path === '/api/candidate/applications' && m === 'GET') {
        const list = db.apps.filter((a) => a.candidateId === active.id).sort((a, b) => b.appliedAt.localeCompare(a.appliedAt));
        const pg = page(list, q); return send(res, 200, { ...pg, content: pg.content.map((a) => ({ ...a, commission: db.commissions.find((c) => c.applicationId === a.id) ? { ...db.commissions.find((c) => c.applicationId === a.id), candidateName: active.fullName } : null })) });
      }
      if ((mm = path.match(/^\/api\/candidate\/applications\/([^/]+)\/withdraw$/)) && m === 'POST') {
        const a = db.apps.find((x) => x.id === mm[1] && x.candidateId === active.id);
        if (!a) return err(res, 404, 'Application not found');
        if (['HIRED', 'REJECTED', 'WITHDRAWN'].includes(a.status)) return err(res, 409, 'This application can no longer be withdrawn');
        a.status = 'WITHDRAWN'; a.updatedAt = now(); return send(res, 200, { ...a, commission: null });
      }
      if (path === '/api/candidate/posts' && m === 'GET') return send(res, 200, db.posts.filter((x) => x.authorId === active.id).map(postView));
      if (path === '/api/candidate/posts' && m === 'POST') { const x = { id: id(), authorId: active.id, type: body.type, title: body.title.trim(), content: body.content.trim(), tags: body.tags || [], status: 'VISIBLE', createdAt: now(), updatedAt: now() }; db.posts.unshift(x); return send(res, 201, postView(x)); }
      if ((mm = path.match(/^\/api\/candidate\/posts\/([^/]+)$/))) {
        const x = db.posts.find((y) => y.id === mm[1] && y.authorId === active.id); if (!x) return err(res, 404, 'Post not found');
        if (m === 'PUT') { Object.assign(x, { type: body.type, title: body.title, content: body.content, tags: body.tags || [], updatedAt: now() }); return send(res, 200, postView(x)); }
        if (m === 'DELETE') { db.posts = db.posts.filter((y) => y !== x); return send(res, 200, { message: 'Post deleted' }); }
      }
    }

    // ---- employer ----
    if (need === 'EMPLOYER') {
      const p = db.emp[active.id];
      const ownerView = () => ({ ...p, email: active.email, userId: active.id, profileComplete: !!empComplete(p) });
      if (path === '/api/employer/profile' && m === 'GET') return send(res, 200, ownerView());
      if (path === '/api/employer/profile' && m === 'PUT') { Object.assign(p, { companyName: body.companyName.trim(), website: body.website || null, industry: body.industry || null, companySize: body.companySize || null, about: body.about || null, country: body.country || null, foundedYear: body.foundedYear ?? null, contactPhone: body.contactPhone || null }); return send(res, 200, ownerView()); }
      if (path === '/api/employer/jobs' && m === 'GET') return send(res, 200, db.jobs.filter((j) => j.employerId === active.id).map((j) => jobView(j, true)));
      if (path === '/api/employer/jobs' && m === 'POST') {
        if (!empComplete(p)) return err(res, 400, 'Complete your company profile (name and description) before posting jobs');
        const j = { id: id(), employerId: active.id, companyName: p.companyName, ...body, hiredCount: 0, status: 'OPEN', createdAt: now(), updatedAt: now() }; db.jobs.unshift(j); return send(res, 201, jobView(j, true));
      }
      if ((mm = path.match(/^\/api\/employer\/jobs\/([^/]+)(\/.*)?$/))) {
        const j = db.jobs.find((x) => x.id === mm[1] && x.employerId === active.id); if (!j) return err(res, 404, 'Job not found');
        const sub = mm[2] || '';
        if (!sub && m === 'PUT') { Object.assign(j, body, { updatedAt: now() }); return send(res, 200, jobView(j, true)); }
        if (!sub && m === 'DELETE') { if (j.hiredCount) return err(res, 409, 'A job with hires cannot be deleted. Close it instead.'); db.jobs = db.jobs.filter((x) => x !== j); return send(res, 200, { message: 'Job deleted' }); }
        if (sub === '/status' && m === 'PATCH') { j.status = body.status; return send(res, 200, jobView(j, true)); }
        if (sub === '/applications') {
          let list = db.apps.filter((a) => a.jobId === j.id); if (q.get('status')) list = list.filter((a) => a.status === q.get('status'));
          list.sort((a, b) => b.matchScore - a.matchScore); const pg = page(list, q);
          return send(res, 200, { ...pg, content: pg.content.map((a) => { const cp = db.cand[a.candidateId]; const cu = userOf(a.candidateId); return { id: a.id, jobId: a.jobId, jobTitle: a.jobTitle, status: a.status, coverNote: a.coverNote, matchScore: a.matchScore, appliedAt: a.appliedAt, updatedAt: a.updatedAt, agreedMonthlySalary: a.agreedMonthlySalary, applicant: { profile: pub(cp), email: cu.email, phone: cp.phone, hasResume: !!db.resumes[cp.userId], resumeFileName: db.resumes[cp.userId]?.name } }; }) });
        }
        if (sub === '/matches') {
          const out = Object.values(db.cand).filter((c) => c.publicProfile && c.accountActive && candComplete(c)).map((c) => ({ candidate: pub(c), ...score(c, j) })).sort((a, b) => b.score - a.score); return send(res, 200, out);
        }
      }
      if ((mm = path.match(/^\/api\/employer\/applications\/([^/]+)\/status$/)) && m === 'PATCH') {
        const a = db.apps.find((x) => x.id === mm[1] && x.employerId === active.id); if (!a) return err(res, 404, 'Application not found');
        if (['HIRED', 'REJECTED', 'WITHDRAWN'].includes(a.status) || a.status === body.status || !['SHORTLISTED', 'INTERVIEW', 'OFFERED', 'HIRED', 'REJECTED'].includes(body.status)) return err(res, 409, `This application cannot move from ${a.status} to ${body.status}`);
        const j = db.jobs.find((x) => x.id === a.jobId);
        if (body.status === 'HIRED') {
          if (!body.agreedMonthlySalary) return err(res, 400, 'Enter the agreed monthly pay to record the hire');
          if (j.hiredCount >= j.openings) return err(res, 409, 'All openings for this job are already filled');
          a.agreedMonthlySalary = body.agreedMonthlySalary; j.hiredCount += 1; if (j.hiredCount >= j.openings) j.status = 'CLOSED';
          const cur = body.currency || j.currency;
          db.commissions.unshift({ id: id(), applicationId: a.id, jobId: j.id, jobTitle: j.title, companyName: j.companyName, candidateId: a.candidateId, candidateName: userOf(a.candidateId).fullName, agreedMonthlySalary: body.agreedMonthlySalary, currency: cur, percent: PERCENT, amount: Math.round(body.agreedMonthlySalary * PERCENT / 100), status: 'PENDING', notes: null, createdAt: now(), paidAt: null });
          log(active, 'HIRE_RECORDED', 'APPLICATION', a.id, `job=${j.id}`);
        }
        a.status = body.status; a.updatedAt = now(); const cp = db.cand[a.candidateId];
        return send(res, 200, { ...a, applicant: { profile: pub(cp), email: userOf(a.candidateId).email, phone: cp.phone, hasResume: !!db.resumes[cp.userId] } });
      }
      if ((mm = path.match(/^\/api\/employer\/candidates\/([^/]+)\/resume$/))) {
        const ok = db.apps.some((a) => a.employerId === active.id && a.candidateId === mm[1]); const f = db.resumes[mm[1]];
        if (!ok || !f) return err(res, 404, 'Resume not found');
        res.writeHead(200, { 'Content-Type': 'application/pdf', 'Content-Disposition': `attachment; filename="${f.name}"` }); return res.end(f.data);
      }
      if (path === '/api/employer/talent' && m === 'GET') {
        let list = Object.values(db.cand).filter((c) => c.publicProfile && c.accountActive && c.headline);
        const t = canon(q.get('q')); if (t) list = list.filter((c) => [c.headline, c.about, ...c.skills, ...c.preferredRoles].some((x) => canon(x).includes(t)));
        if (q.get('skill')) list = list.filter((c) => c.skills.some((s) => canon(s) === canon(q.get('skill'))));
        if (q.get('level')) list = list.filter((c) => c.experienceLevel === q.get('level'));
        if (q.get('type')) list = list.filter((c) => c.preferredJobTypes.includes(q.get('type')));
        const pg = page(list, q); return send(res, 200, { ...pg, content: pg.content.map(pub) });
      }
      if ((mm = path.match(/^\/api\/employer\/talent\/([^/]+)$/))) {
        const c = db.cand[mm[1]]; const applied = db.apps.some((a) => a.employerId === active.id && a.candidateId === mm[1]);
        if (!c || !c.accountActive || (!c.publicProfile && !applied)) return err(res, 404, 'Candidate not found');
        return send(res, 200, { profile: pub(c), posts: db.posts.filter((x) => x.authorId === c.userId && x.status === 'VISIBLE').map(postView) });
      }
      if (path === '/api/employer/posts') {
        const vis = new Set(Object.values(db.cand).filter((c) => c.publicProfile && c.accountActive).map((c) => c.userId));
        let list = db.posts.filter((x) => x.status === 'VISIBLE' && vis.has(x.authorId)); const t = canon(q.get('q'));
        if (t) list = list.filter((x) => [x.title, x.content, ...x.tags].some((s) => canon(s).includes(t)));
        const pg = page(list, q); return send(res, 200, { ...pg, content: pg.content.map(postView) });
      }
    }

    // ---- admin ----
    if (need === 'ADMIN') {
      if (path === '/api/admin/stats') return send(res, 200, { candidates: db.users.filter((u) => u.role === 'CANDIDATE').length, employers: db.users.filter((u) => u.role === 'EMPLOYER').length, suspendedUsers: db.users.filter((u) => u.status === 'SUSPENDED').length, openJobs: db.jobs.filter((j) => j.status === 'OPEN').length, applications: db.apps.length, hires: db.apps.filter((a) => a.status === 'HIRED').length, pendingCommissions: db.commissions.filter((c) => c.status === 'PENDING').length });
      if (path === '/api/admin/users' && m === 'GET') {
        let list = db.users.filter((u) => u.role !== 'ADMIN'); const t = canon(q.get('q'));
        if (t) list = list.filter((u) => canon(u.email).includes(t) || canon(u.fullName).includes(t));
        if (q.get('role')) list = list.filter((u) => u.role === q.get('role')); if (q.get('status')) list = list.filter((u) => u.status === q.get('status'));
        const pg = page(list, q); return send(res, 200, { ...pg, content: pg.content.map(userView) });
      }
      if ((mm = path.match(/^\/api\/admin\/users\/([^/]+)(\/status)?$/))) {
        const u = userOf(mm[1]); if (!u || u.role === 'ADMIN') return err(res, 404, 'User not found');
        if (m === 'GET') return send(res, 200, { user: userView(u), candidate: db.cand[u.id] ? pub(db.cand[u.id]) : null, employer: db.emp[u.id] ? (({ contactPhone, ...rest }) => rest)(db.emp[u.id]) : null, jobs: db.jobs.filter((j) => j.employerId === u.id).length, applications: db.apps.filter((a) => a.candidateId === u.id).length, posts: db.posts.filter((p) => p.authorId === u.id).length });
        if (m === 'PATCH') { u.status = body.status; if (db.cand[u.id]) db.cand[u.id].accountActive = body.status === 'ACTIVE'; if (body.status === 'SUSPENDED') for (const [t, uid] of Object.entries(db.tokens)) if (uid === u.id) delete db.tokens[t]; log(active, `USER_${body.status}`, 'USER', u.id, u.email); return send(res, 200, userView(u)); }
        if (m === 'DELETE') { db.users = db.users.filter((x) => x !== u); delete db.cand[u.id]; delete db.emp[u.id]; log(active, 'USER_DELETED', 'USER', u.id, u.email); return send(res, 200, { message: 'User and all their data were deleted' }); }
      }
      if ((mm = path.match(/^\/api\/admin\/employers\/([^/]+)\/verify$/)) && m === 'PATCH') { const e = db.emp[mm[1]]; if (!e) return err(res, 404, 'Employer not found'); e.verified = !!body.verified; return send(res, 200, e); }
      if (path === '/api/admin/jobs' && m === 'GET') { const pg = page(db.jobs, q); return send(res, 200, { ...pg, content: pg.content.map((j) => jobView(j, true)) }); }
      if ((mm = path.match(/^\/api\/admin\/jobs\/([^/]+)\/status$/)) && m === 'PATCH') { const j = db.jobs.find((x) => x.id === mm[1]); if (!j) return err(res, 404, 'Job not found'); j.status = body.status; log(active, `JOB_STATUS_${body.status}`, 'JOB', j.id, j.title); return send(res, 200, jobView(j, true)); }
      if (path === '/api/admin/posts' && m === 'GET') { const pg = page(db.posts, q); return send(res, 200, { ...pg, content: pg.content.map(postView) }); }
      if ((mm = path.match(/^\/api\/admin\/posts\/([^/]+)\/status$/)) && m === 'PATCH') { const x = db.posts.find((y) => y.id === mm[1]); if (!x) return err(res, 404, 'Post not found'); x.status = body.status; return send(res, 200, postView(x)); }
      if (path === '/api/admin/commissions' && m === 'GET') { let list = db.commissions; if (q.get('status')) list = list.filter((c) => c.status === q.get('status')); return send(res, 200, page(list, q)); }
      if ((mm = path.match(/^\/api\/admin\/commissions\/([^/]+)$/)) && m === 'PATCH') { const c = db.commissions.find((x) => x.id === mm[1]); if (!c) return err(res, 404, 'Commission not found'); c.status = body.status; c.paidAt = body.status === 'PAID' ? now() : null; log(active, `COMMISSION_${body.status}`, 'COMMISSION', c.id, ''); return send(res, 200, c); }
      if (path === '/api/admin/audit') return send(res, 200, page(db.audit, q));
    }
    return err(res, 404, 'The request could not be processed');
  } catch (e) {
    console.error(e); err(res, 500, 'Something went wrong. Please try again later.');
  }
}).listen(PORT, () => console.log(`Mock API on http://localhost:${PORT}`));
