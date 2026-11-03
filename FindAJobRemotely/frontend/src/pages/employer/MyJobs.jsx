import { Badge, Button, Group, Menu, Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { IconDots } from '@tabler/icons-react';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';
import { jobTypeLabel, timeAgo } from '../../lib/format.js';

const STATUS = { OPEN: ['teal', 'Open'], CLOSED: ['gray', 'Closed'], REMOVED: ['red', 'Removed by admin'] };

export default function MyJobs() {
  const [jobs, setJobs] = useState(null);
  const [error, setError] = useState('');
  const load = useCallback(() => { api.get('/employer/jobs').then((r) => setJobs(r.data)).catch((e) => setError(errorMessage(e))); }, []);
  useEffect(() => { load(); }, [load]);

  const setStatus = async (job, status) => {
    try { await api.patch(`/employer/jobs/${job.id}/status`, { status }); load(); }
    catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
  };
  const remove = (job) => modals.openConfirmModal({
    title: 'Delete this job?', children: <Text size="sm">“{job.title}” and its applications will be deleted.</Text>,
    labels: { confirm: 'Delete', cancel: 'Keep it' }, confirmProps: { color: 'red' },
    onConfirm: async () => {
      try { await api.delete(`/employer/jobs/${job.id}`); load(); }
      catch (e) { notifications.show({ color: 'red', title: 'Could not delete', message: errorMessage(e) }); }
    },
  });

  return (
    <>
      <PageHeader title="My jobs" subtitle="Review applicants and manage your open roles." actions={<Button component={Link} to="/employer/post-job">Post a job</Button>} />
      <ErrorNote message={error} />
      {!jobs && !error && <PageLoader />}
      {jobs && jobs.length === 0 && <EmptyState title="You have not posted a job yet" text="Post your first role to start receiving applications." actionLabel="Post a job" to="/employer/post-job" />}
      {jobs && jobs.length > 0 && (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">
          {jobs.map((j) => (
            <li key={j.id} className="row-link flex items-start gap-4 border-b border-slate-200 px-2 py-4 last:border-b-0 sm:px-4">
              <div className="min-w-0 flex-1">
                <Link to={`/employer/jobs/${j.id}`} className="font-display text-lg font-bold no-underline hover:underline">{j.title}</Link>
                <Group gap={8} mt={4}>
                  <Badge color={STATUS[j.status][0]} variant="light" radius="sm">{STATUS[j.status][1]}</Badge>
                  <Text size="sm" c="dimmed">{jobTypeLabel(j.type)} · posted {timeAgo(j.createdAt)}</Text>
                </Group>
                <Text size="sm" mt={6}>
                  <strong>{j.applicantCount ?? 0}</strong> {j.applicantCount === 1 ? 'applicant' : 'applicants'} · <strong>{j.hiredCount}</strong> of {j.openings} hired
                </Text>
              </div>
              <Group gap="xs" wrap="nowrap">
                <Button component={Link} to={`/employer/jobs/${j.id}`} variant="light" size="compact-md">Applicants</Button>
                <Menu position="bottom-end">
                  <Menu.Target><Button variant="subtle" color="dark" size="compact-md" px={6} aria-label={`More actions for ${j.title}`}><IconDots size={18} /></Button></Menu.Target>
                  <Menu.Dropdown>
                    <Menu.Item component={Link} to={`/employer/jobs/${j.id}/edit`} disabled={j.status === 'REMOVED'}>Edit</Menu.Item>
                    <Menu.Item component={Link} to={`/employer/jobs/${j.id}/matches`}>Best-fit candidates</Menu.Item>
                    {j.status === 'OPEN' && <Menu.Item onClick={() => setStatus(j, 'CLOSED')}>Close job</Menu.Item>}
                    {j.status === 'CLOSED' && <Menu.Item onClick={() => setStatus(j, 'OPEN')}>Reopen job</Menu.Item>}
                    <Menu.Divider />
                    <Menu.Item color="red" onClick={() => remove(j)}>Delete</Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              </Group>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
