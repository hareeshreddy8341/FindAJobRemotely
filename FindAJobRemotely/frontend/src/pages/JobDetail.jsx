import { Badge, Button, Group, Stack, Text, Textarea, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconRosetteDiscountCheckFilled } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client.js';
import { ErrorNote, PageLoader } from '../components/ui.jsx';
import { jobTypeLabel, levelLabel, payRange, timeAgo } from '../lib/format.js';

export default function JobDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useSelector((s) => s.auth.user);
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [applied, setApplied] = useState(false);

  useEffect(() => {
    api.get(`/jobs/${id}`).then((r) => setJob(r.data)).catch((e) => setError(errorMessage(e, 'This job could not be loaded.')));
  }, [id, user?.id]);

  const apply = async () => {
    setBusy(true);
    try {
      await api.post(`/candidate/jobs/${id}/apply`, { coverNote: note.trim() || null });
      setApplied(true);
      notifications.show({ color: 'teal', title: 'Application sent', message: `You applied to ${job.title}.` });
    } catch (e) {
      if (e.response?.status === 409) setApplied(true);
      notifications.show({ color: e.response?.status === 409 ? 'blue' : 'red', title: 'Could not apply', message: errorMessage(e) });
    } finally {
      setBusy(false);
    }
  };

  if (error) return <ErrorNote message={error} />;
  if (!job) return <PageLoader label="Loading job" />;

  return (
    <article className="grid gap-8 lg:grid-cols-[1fr_320px]">
      <div className="min-w-0">
        <Button variant="subtle" color="dark" px={0} onClick={() => navigate(-1)}>Back</Button>
        <Title order={1} mt="xs" style={{ fontSize: 'clamp(1.8rem, 4vw, 2.6rem)' }}>{job.title}</Title>
        <Group gap={6} mt="xs">
          <Text fw={600}>{job.companyName}</Text>
          {job.companyVerified && <IconRosetteDiscountCheckFilled size={18} color="#2F4BFF" aria-label="Verified company" />}
          <Text c="dimmed">· {job.remoteRegion} · posted {timeAgo(job.createdAt)}</Text>
        </Group>
        {job.status !== 'OPEN' && <Badge mt="sm" color="red" variant="light">This job is {job.status === 'CLOSED' ? 'closed' : 'removed'}</Badge>}
        <Group gap={8} mt="md">
          <Badge variant="light" color="gray" radius="sm">{jobTypeLabel(job.type)}</Badge>
          <Badge variant="light" color="gray" radius="sm">{levelLabel(job.experienceLevel)}</Badge>
          <Text size="sm" c="dimmed">{payRange(job)}</Text>
        </Group>
        <Title order={2} size="h4" mt="xl">About the role</Title>
        {/* Rendered as text with preserved line breaks - never as HTML */}
        <Text mt="xs" style={{ whiteSpace: 'pre-wrap', lineHeight: 1.65 }} maw={680}>{job.description}</Text>
        <Title order={2} size="h4" mt="xl">Skills</Title>
        <Group gap={6} mt="xs">
          {job.skills.map((s) => <Badge key={s} variant="outline" color="signal" radius="sm" tt="none" fw={500}>{s}</Badge>)}
        </Group>
      </div>

      <aside className="h-fit rounded-xl border border-slate-200 bg-white p-5 lg:sticky lg:top-24" aria-label="Apply">
        {job.status !== 'OPEN' ? (
          <Text>This job is not accepting applications.</Text>
        ) : !user ? (
          <Stack>
            <Title order={2} size="h4">Interested?</Title>
            <Text size="sm" c="dimmed">Create a free profile to apply. You pay only if you get hired.</Text>
            <Button component={Link} to="/register">Create profile</Button>
            <Button component={Link} to="/login" state={{ from: `/jobs/${id}` }} variant="default">Sign in</Button>
          </Stack>
        ) : user.role === 'CANDIDATE' ? (
          applied ? (
            <Stack>
              <Title order={2} size="h4">Applied</Title>
              <Text size="sm" c="dimmed">The company can now see your contact details and resume.</Text>
              <Button component={Link} to="/candidate/applications" variant="default">View applied jobs</Button>
            </Stack>
          ) : (
            <Stack>
              <Title order={2} size="h4">Apply for this job</Title>
              <Textarea label="Note to the company (optional)" autosize minRows={3} maxRows={8} maxLength={2000}
                value={note} onChange={(e) => setNote(e.currentTarget.value)} />
              <Text size="xs" c="dimmed">Applying shares your email, phone number and resume with this company.</Text>
              <Button onClick={apply} loading={busy}>Apply</Button>
            </Stack>
          )
        ) : (
          <Text size="sm" c="dimmed">
            {user.role === 'EMPLOYER' ? 'Employers cannot apply to jobs.' : 'Administrators cannot apply to jobs.'}
          </Text>
        )}
      </aside>
    </article>
  );
}
