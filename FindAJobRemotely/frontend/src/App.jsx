import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { Route, Routes } from 'react-router-dom';
import AdminLayout from './components/AdminLayout.jsx';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import { bootstrapSession } from './store/authThunks.js';

import Home from './pages/Home.jsx';
import Jobs from './pages/Jobs.jsx';
import JobDetail from './pages/JobDetail.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Account from './pages/Account.jsx';
import { HowItWorks, NotFound, Privacy } from './pages/Static.jsx';

import FindJobs from './pages/candidate/FindJobs.jsx';
import Applications from './pages/candidate/Applications.jsx';
import Posts from './pages/candidate/Posts.jsx';
import CandidateProfile from './pages/candidate/Profile.jsx';

import EmployerProfile from './pages/employer/Profile.jsx';
import PostJob from './pages/employer/PostJob.jsx';
import MyJobs from './pages/employer/MyJobs.jsx';
import Applicants from './pages/employer/Applicants.jsx';
import Matches from './pages/employer/Matches.jsx';
import Talent from './pages/employer/Talent.jsx';
import TalentDetail from './pages/employer/TalentDetail.jsx';
import CandidatePosts from './pages/employer/CandidatePosts.jsx';

import Overview from './pages/admin/Overview.jsx';
import Users from './pages/admin/Users.jsx';
import AdminJobs from './pages/admin/Jobs.jsx';
import AdminPosts from './pages/admin/Posts.jsx';
import Commissions from './pages/admin/Commissions.jsx';
import Audit from './pages/admin/Audit.jsx';

export default function App() {
  const dispatch = useDispatch();
  useEffect(() => { dispatch(bootstrapSession()); }, [dispatch]);

  return (
    <Routes>
      {/* Separate admin portal: its own sign-in and its own shell */}
      <Route path="/admin/login" element={<div className="min-h-screen bg-paper px-4 py-10"><Login admin /></div>} />
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<Overview />} />
          <Route path="/admin/users" element={<Users />} />
          <Route path="/admin/jobs" element={<AdminJobs />} />
          <Route path="/admin/posts" element={<AdminPosts />} />
          <Route path="/admin/commissions" element={<Commissions />} />
          <Route path="/admin/audit" element={<Audit />} />
        </Route>
      </Route>

      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/:id" element={<JobDetail />} />
        <Route path="/how-it-works" element={<HowItWorks />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route element={<ProtectedRoute roles={['CANDIDATE', 'EMPLOYER']} />}>
          <Route path="/account" element={<Account />} />
        </Route>

        <Route element={<ProtectedRoute roles={['CANDIDATE']} />}>
          <Route path="/candidate/jobs" element={<FindJobs />} />
          <Route path="/candidate/applications" element={<Applications />} />
          <Route path="/candidate/posts" element={<Posts />} />
          <Route path="/candidate/profile" element={<CandidateProfile />} />
        </Route>

        <Route element={<ProtectedRoute roles={['EMPLOYER']} />}>
          <Route path="/employer/profile" element={<EmployerProfile />} />
          <Route path="/employer/post-job" element={<PostJob />} />
          <Route path="/employer/jobs" element={<MyJobs />} />
          <Route path="/employer/jobs/:id" element={<Applicants />} />
          <Route path="/employer/jobs/:id/edit" element={<PostJob />} />
          <Route path="/employer/jobs/:id/matches" element={<Matches />} />
          <Route path="/employer/talent" element={<Talent />} />
          <Route path="/employer/talent/:id" element={<TalentDetail />} />
          <Route path="/employer/posts" element={<CandidatePosts />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
