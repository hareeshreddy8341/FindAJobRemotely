import { Button, Group, Text, TextInput, Title } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import WorldClock from '../components/WorldClock.jsx';
import { setFilters } from '../store/jobsSlice.js';
import { roleHome } from '../lib/format.js';

const STEPS = [
  { t: 'Build your profile once', d: 'Add your skills, experience and the kind of work you want. Phone, pay expectations and resume stay private.' },
  { t: 'We match you to roles', d: 'Every open job gets a fit score, and we show which skills you already have and which are missing.' },
  { t: 'Apply and track', d: 'Apply in one click and follow each application from applied to hired.' },
  { t: 'Pay only when you are hired', d: 'Our fee is a small percentage of your first month’s pay, and it is only charged after a company records your hire.' },
];

export default function Home() {
  const user = useSelector((s) => s.auth.user);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [q, setQ] = useState('');

  if (user) return <Navigate to={roleHome(user.role)} replace />;

  const search = (e) => {
    e.preventDefault();
    dispatch(setFilters({ q: q.trim(), page: 0 }));
    navigate('/jobs');
  };

  return (
    <div className="space-y-16">
      <section className="pt-4">
        <Title order={1} className="max-w-3xl" style={{ fontSize: 'clamp(2.2rem, 5.5vw, 4rem)', lineHeight: 1.05 }}>
          Remote jobs at startups. You pay only if you get hired.
        </Title>
        <Text mt="md" size="lg" maw={620} c="dimmed">
          Candidates look for remote jobs and internships for free. Startups post roles and find people through
          profiles and posts. We earn a small commission only when a hire happens.
        </Text>

        <form onSubmit={search} role="search" className="mt-7 flex max-w-xl flex-col gap-2 sm:flex-row">
          <TextInput className="flex-1" size="lg" aria-label="Search remote jobs" placeholder="Try React, data analyst or design intern"
            leftSection={<IconSearch size={18} />} value={q} onChange={(e) => setQ(e.currentTarget.value)} maxLength={80} />
          <Button type="submit" size="lg">Search jobs</Button>
        </form>
        <Group mt="sm" gap="sm">
          <Button component={Link} to="/register?role=EMPLOYER" variant="subtle" color="dark" px={0}>I am hiring</Button>
        </Group>

        <div className="mt-12">
          <Text fw={600} mb="sm">Where the work happens right now</Text>
          <WorldClock />
        </div>
      </section>

      <section aria-labelledby="how">
        <Title order={2} id="how" size="h3" mb="md">How it works for candidates</Title>
        <ol className="m-0 grid list-none gap-6 p-0 md:grid-cols-2">
          {STEPS.map((s, i) => (
            <li key={s.t} className="flex gap-4">
              <span aria-hidden="true" className="w-8 shrink-0 font-display text-3xl font-bold text-signal">{i + 1}</span>
              <div>
                <Title order={3} size="h5">{s.t}</Title>
                <Text c="dimmed" mt={4}>{s.d}</Text>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="rounded-2xl bg-ink p-6 text-white md:p-10" aria-labelledby="hire">
        <Title order={2} id="hire" size="h3" c="white">Hiring a remote team?</Title>
        <Text mt="xs" maw={560} c="gray.3">
          Post a role in minutes, browse candidate profiles and project posts, and see a fit score for every applicant.
          Contact details unlock only for people who apply to you.
        </Text>
        <Button mt="md" component={Link} to="/register?role=EMPLOYER" variant="white" color="dark" size="md">Create a company profile</Button>
      </section>
    </div>
  );
}
