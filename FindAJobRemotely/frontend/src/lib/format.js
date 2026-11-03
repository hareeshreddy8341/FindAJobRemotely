export const JOB_TYPES = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
];
export const LEVELS = [
  { value: 'FRESHER', label: 'Fresher' },
  { value: 'JUNIOR', label: 'Junior' },
  { value: 'MID', label: 'Mid-level' },
  { value: 'SENIOR', label: 'Senior' },
];
export const POST_TYPES = [
  { value: 'PROJECT', label: 'Project' },
  { value: 'ACHIEVEMENT', label: 'Achievement' },
  { value: 'AVAILABILITY', label: 'Availability' },
  { value: 'ARTICLE', label: 'Article' },
];
export const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP', 'CAD', 'AUD', 'SGD', 'AED', 'NGN', 'BRL'];

const labelOf = (list) => (v) => list.find((x) => x.value === v)?.label ?? v ?? '';
export const jobTypeLabel = labelOf(JOB_TYPES);
export const levelLabel = labelOf(LEVELS);
export const postTypeLabel = labelOf(POST_TYPES);

export const STATUS_LABEL = {
  APPLIED: 'Applied', SHORTLISTED: 'Shortlisted', INTERVIEW: 'Interview', OFFERED: 'Offer made',
  HIRED: 'Hired', REJECTED: 'Not selected', WITHDRAWN: 'Withdrawn',
};
export const STATUS_COLOR = {
  APPLIED: 'gray', SHORTLISTED: 'signal', INTERVIEW: 'violet', OFFERED: 'orange',
  HIRED: 'teal', REJECTED: 'red', WITHDRAWN: 'gray',
};

export function money(n, currency) {
  if (n == null) return '';
  try {
    return new Intl.NumberFormat('en', { style: 'currency', currency: currency || 'USD', maximumFractionDigits: 0 }).format(n);
  } catch {
    return `${n} ${currency || ''}`.trim();
  }
}

export function payRange(job) {
  const { salaryMin: a, salaryMax: b, currency } = job;
  if (a == null && b == null) return job.type === 'INTERNSHIP' ? 'Stipend not listed' : 'Pay not listed';
  if (a != null && b != null) return `${money(a, currency)} to ${money(b, currency)} a month`;
  return `${money(a ?? b, currency)} a month`;
}

export function timeAgo(iso) {
  if (!iso) return '';
  const s = Math.max(1, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 60) return 'just now';
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h} h ago`;
  const d = Math.floor(h / 24);
  if (d < 30) return `${d} day${d === 1 ? '' : 's'} ago`;
  return new Date(iso).toLocaleDateString('en', { day: 'numeric', month: 'short', year: 'numeric' });
}

export const fullDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';

export const roleHome = (role) =>
  role === 'ADMIN' ? '/admin' : role === 'EMPLOYER' ? '/employer/jobs' : '/candidate/jobs';
