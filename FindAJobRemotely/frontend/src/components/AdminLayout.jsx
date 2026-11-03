import { Button } from '@mantine/core';
import { IconCash, IconBriefcase, IconChecklist, IconLayoutDashboard, IconLogout, IconMessage, IconUsers } from '@tabler/icons-react';
import { useDispatch, useSelector } from 'react-redux';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import Logo from './Logo.jsx';
import { logout } from '../store/authThunks.js';

const ITEMS = [
  { to: '/admin', label: 'Overview', icon: IconLayoutDashboard, end: true },
  { to: '/admin/users', label: 'Users', icon: IconUsers },
  { to: '/admin/jobs', label: 'Jobs', icon: IconBriefcase },
  { to: '/admin/posts', label: 'Candidate posts', icon: IconMessage },
  { to: '/admin/commissions', label: 'Commissions', icon: IconCash },
  { to: '/admin/audit', label: 'Audit log', icon: IconChecklist },
];

const cls = ({ isActive }) =>
  `flex items-center gap-2 rounded-md px-3 py-2 text-[15px] font-semibold no-underline ${
    isActive ? 'bg-white text-ink' : 'text-slate-300 hover:bg-white/10'
  }`;

/** The admin portal has its own shell, so it never looks like the public site. */
export default function AdminLayout() {
  const user = useSelector((s) => s.auth.user);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const signOut = async () => { await dispatch(logout()); navigate('/admin/login'); };

  return (
    <div className="min-h-screen md:flex">
      <a href="#main" className="skip-link">Skip to content</a>
      <aside className="bg-ink px-4 py-5 md:sticky md:top-0 md:h-screen md:w-64 md:shrink-0">
        <Logo to="/admin" light />
        <p className="mt-1 text-sm text-slate-400">Admin portal</p>
        <nav aria-label="Admin" className="mt-5 flex gap-1 overflow-x-auto md:flex-col md:overflow-visible">
          {ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={cls}>
              <Icon size={18} aria-hidden="true" /> <span className="whitespace-nowrap">{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="mt-6 border-t border-white/15 pt-4 text-sm text-slate-300 md:absolute md:bottom-5 md:left-4 md:right-4">
          <div className="truncate">{user?.email}</div>
          <Button mt="xs" size="xs" variant="white" color="dark" leftSection={<IconLogout size={14} />} onClick={signOut}>Sign out</Button>
        </div>
      </aside>
      <main id="main" className="min-w-0 flex-1 px-4 py-8 md:px-8">
        <div className="mx-auto max-w-5xl"><Outlet /></div>
      </main>
    </div>
  );
}
