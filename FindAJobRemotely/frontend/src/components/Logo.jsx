import { Link } from 'react-router-dom';

export default function Logo({ to = '/', light = false }) {
  return (
    <Link to={to} className="inline-flex items-center gap-2 no-underline" aria-label="FindAJobRemotely home">
      <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true">
        <rect width="32" height="32" rx="8" fill={light ? '#fff' : '#0F1B2D'} />
        <circle cx="16" cy="16" r="8" fill="none" stroke={light ? '#0F1B2D' : '#fff'} strokeWidth="2.5" />
        <path d="M16 11v5l3 2" stroke="#FFB547" strokeWidth="2.5" strokeLinecap="round" fill="none" />
      </svg>
      <span className="font-display text-lg font-bold tracking-tight" style={{ color: light ? '#fff' : '#0F1B2D' }}>
        FindAJobRemotely
      </span>
    </Link>
  );
}
