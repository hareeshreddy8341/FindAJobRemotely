import { useDisclosure } from '@mantine/hooks';
import { Burger, Button, Drawer, Group, Menu, Stack } from '@mantine/core';
import { IconChevronDown, IconLogout, IconUser } from '@tabler/icons-react';
import { useDispatch, useSelector } from 'react-redux';
import { NavLink, useNavigate } from 'react-router-dom';
import Logo from './Logo.jsx';
import { logout } from '../store/authThunks.js';
import { roleHome } from '../lib/format.js';

const LINKS = {
  CANDIDATE: [
    { to: '/candidate/jobs', label: 'Find jobs' },
    { to: '/candidate/applications', label: 'Applied jobs' },
    { to: '/candidate/posts', label: 'My posts' },
  ],
  EMPLOYER: [
    { to: '/employer/post-job', label: 'Post a job' },
    { to: '/employer/jobs', label: 'My jobs' },
    { to: '/employer/posts', label: 'Candidate posts' },
    { to: '/employer/talent', label: 'Find talent' },
  ],
  GUEST: [
    { to: '/jobs', label: 'Browse jobs' },
    { to: '/how-it-works', label: 'How it works' },
  ],
};

const linkClass = ({ isActive }) =>
  `rounded-md px-3 py-2 text-[15px] font-semibold no-underline transition-colors ${
    isActive ? 'bg-ink text-white' : 'text-ink hover:bg-white'
  }`;

export default function Navbar() {
  const { user } = useSelector((s) => s.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [opened, { toggle, close }] = useDisclosure(false);

  const links = LINKS[user?.role] || LINKS.GUEST;
  const signOut = async () => {
    close();
    await dispatch(logout());
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-paper/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
        <Logo to={user ? roleHome(user.role) : '/'} />

        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className={linkClass}>{l.label}</NavLink>
          ))}
        </nav>

        <div className="hidden items-center gap-2 md:flex">
          {user ? (
            <Menu position="bottom-end" width={220}>
              <Menu.Target>
                <Button variant="default" leftSection={<IconUser size={16} />} rightSection={<IconChevronDown size={14} />}>
                  {user.fullName.split(' ')[0]}
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Label>{user.email}</Menu.Label>
                <Menu.Item component={NavLink} to={user.role === 'EMPLOYER' ? '/employer/profile' : '/candidate/profile'}>
                  {user.role === 'EMPLOYER' ? 'Company profile' : 'My profile'}
                </Menu.Item>
                <Menu.Item component={NavLink} to="/account">Account and security</Menu.Item>
                <Menu.Divider />
                <Menu.Item color="red" leftSection={<IconLogout size={16} />} onClick={signOut}>Sign out</Menu.Item>
              </Menu.Dropdown>
            </Menu>
          ) : (
            <>
              <Button variant="subtle" color="dark" component={NavLink} to="/login">Sign in</Button>
              <Button component={NavLink} to="/register">Create profile</Button>
            </>
          )}
        </div>

        <Burger opened={opened} onClick={toggle} hiddenFrom="md" aria-label="Open menu" />
      </div>

      <Drawer opened={opened} onClose={close} position="right" size="xs" title="Menu" hiddenFrom="md">
        <Stack gap={4}>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} onClick={close} className={linkClass}>{l.label}</NavLink>
          ))}
          {user ? (
            <>
              <NavLink to={user.role === 'EMPLOYER' ? '/employer/profile' : '/candidate/profile'} onClick={close} className={linkClass}>
                {user.role === 'EMPLOYER' ? 'Company profile' : 'My profile'}
              </NavLink>
              <NavLink to="/account" onClick={close} className={linkClass}>Account and security</NavLink>
              <Button mt="md" variant="default" onClick={signOut} leftSection={<IconLogout size={16} />}>Sign out</Button>
            </>
          ) : (
            <Group mt="md" grow>
              <Button variant="default" component={NavLink} to="/login" onClick={close}>Sign in</Button>
              <Button component={NavLink} to="/register" onClick={close}>Create profile</Button>
            </Group>
          )}
        </Stack>
      </Drawer>
    </header>
  );
}
