import { useSelector } from 'react-redux';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { PageLoader } from './ui.jsx';
import { roleHome } from '../lib/format.js';

/** Renders its children only for signed-in users with one of the allowed roles. */
export default function ProtectedRoute({ roles }) {
  const { user, status } = useSelector((s) => s.auth);
  const location = useLocation();

  if (status === 'loading') return <PageLoader label="Checking your session" />;
  if (!user) {
    const loginPath = roles?.includes('ADMIN') ? '/admin/login' : '/login';
    return <Navigate to={loginPath} replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) return <Navigate to={roleHome(user.role)} replace />;
  return <Outlet />;
}
