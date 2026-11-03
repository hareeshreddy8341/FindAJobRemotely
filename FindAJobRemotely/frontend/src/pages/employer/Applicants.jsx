import { Alert, Button, Group, Menu, Modal, NumberInput, Select, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconChevronDown } from '@tabler/icons-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, MatchRing, PageHeader, PageLoader, Pager, StatusBadge } from '../../components/ui.jsx';
import { CURRENCIES, STATUS_LABEL, fullDate, money } from '../../lib/format.js';
import { downloadPdf } from '../../lib/download.js';

const MOVES = ['SHORTLISTED', 'INTERVIEW', 'OFFERED', 'HIRED', 'REJECTED'];
const TERMINAL = ['HIRED', 'REJECTED', 'WITHDRAWN'];

export default function Applicants() {
  const { id } = useParams();
  const [job, setJob] = useState(null);
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState(null);
  const [error, setError] = useState('');
  const [hiring, setHiring] = useState(null);
  const [pay, setPay] = useState('');
  const [currency, setCurrency] = useState(null);
  const [busy, setBusy] = useState(false);

  const loadJob = useCallback(() => api.get(`/jobs/${id}`).then((r) => { setJob(r.data); setCurrency((c) => c || r.data.currency); }).catch((e) => setError(errorMessage(e))), [id]);
  const loadApps = useCallback(() => {
    api.get(`/employer/jobs/${id}/applications`, { params: { page, size: 10, ...(filter ? { status: filter } : {}) } })
      .then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [id, page, filter]);
  useEffect(() => { loadJob(); }, [loadJob]);
  useEffect(() => { loadApps(); }, [loadApps]);

  const move = async (app, status, extra = {}) => {
    setBusy(true);
    try {
      await api.patch(`/employer/applications/${app.id}/status`, { status, ...extra });
      notifications.show({ color: 'teal', title: status === 'HIRED' ? 'Hire recorded' : 'Status updated', message: `${app.applicant.profile.fullName} is now: ${STATUS_LABEL[status]}.` });
      setHiring(null);
      await Promise.all([loadApps(), loadJob()]);
    } catch (e) {
      notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) });
    } finally { setBusy(false); }
  };

  if (error) return <ErrorNote message={error} />;
  if (!job) return <PageLoader />;

  return (
    <>
      <PageHeader title={`Applicants for ${job.title}`} subtitle={`${job.hiredCount} of ${job.openings} openings filled. Applicants are listed best match first.`}
        actions={[
          <Button key="m" component={Link} to={`/employer/jobs/${id}/matches`} variant="default">Best-fit candidates</Button>,
          <Button key="b" component={Link} to="/employer/jobs" variant="subtle" color="dark">Back to jobs</Button>,
        ]} />
      <Select aria-label="Filter by status" placeholder="All statuses" clearable w={220} mb="md" value={filter}
        data={Object.entries(STATUS_LABEL).map(([value, label]) => ({ value, label }))}
        onChange={(v) => { setFilter(v); setPage(0); }} />

      {!data && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No applicants here yet" text="Applicants appear as soon as candidates apply. Shortlist the best fits and record hires here." />}
      {data && data.content.length > 0 && (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">
          {data.content.map((a) => {
            const p = a.applicant.profile;
            return (
              <li key={a.id} className="border-b border-slate-200 px-2 py-4 last:border-b-0 sm:px-4">
                <div className="flex items-start gap-4">
                  <MatchRing score={a.matchScore} />
                  <div className="min-w-0 flex-1">
                    <Link to={`/employer/talent/${p.userId}`} className="font-display text-lg font-bold no-underline hover:underline">{p.fullName}</Link>
                    <Text size="sm" fw={500}>{p.headline}</Text>
                    <Text size="sm" c="dimmed">Applied {fullDate(a.appliedAt)}</Text>
                    <Group gap="md" mt={6}>
                      <Text size="sm">Email: <a href={`mailto:${a.applicant.email}`}>{a.applicant.email}</a></Text>
                      {a.applicant.phone && <Text size="sm">Phone: {a.applicant.phone}</Text>}
                    </Group>
                    {a.coverNote && <Text size="sm" mt={6} style={{ whiteSpace: 'pre-wrap' }}>“{a.coverNote}”</Text>}
                    <Group gap="xs" mt={8}>
                      {a.applicant.hasResume && (
                        <Button size="compact-sm" variant="default" onClick={() => downloadPdf(`/employer/candidates/${p.userId}/resume`, a.applicant.resumeFileName || 'resume.pdf').catch((e) => notifications.show({ color: 'red', title: 'Download failed', message: errorMessage(e) }))}>Download resume</Button>
                      )}
                      {a.status === 'HIRED' && a.agreedMonthlySalary && <Text size="sm" c="teal.8" fw={600}>Agreed pay {money(a.agreedMonthlySalary, currency)} a month</Text>}
                    </Group>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <StatusBadge status={a.status} />
                    {!TERMINAL.includes(a.status) && (
                      <Menu position="bottom-end">
                        <Menu.Target><Button size="compact-sm" variant="light" rightSection={<IconChevronDown size={14} />} loading={busy}>Move to</Button></Menu.Target>
                        <Menu.Dropdown>
                          {MOVES.filter((s) => s !== a.status).map((s) => (
                            <Menu.Item key={s} color={s === 'REJECTED' ? 'red' : undefined}
                              onClick={() => (s === 'HIRED' ? (setHiring(a), setPay('')) : move(a, s))}>{STATUS_LABEL[s]}</Menu.Item>
                          ))}
                        </Menu.Dropdown>
                      </Menu>
                    )}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <Modal opened={Boolean(hiring)} onClose={() => setHiring(null)} title={`Record hire: ${hiring?.applicant.profile.fullName ?? ''}`}>
        <Stack>
          <Alert color="blue" variant="light">
            Enter the monthly pay you agreed with the candidate. Our commission is calculated from it and charged to the candidate, not to you.
          </Alert>
          <NumberInput label="Agreed monthly pay" min={1} allowDecimal={false} thousandSeparator="," value={pay} onChange={setPay} data-autofocus />
          <Select label="Currency" data={CURRENCIES} value={currency} onChange={setCurrency} />
          <Button loading={busy} disabled={!pay || !currency} onClick={() => move(hiring, 'HIRED', { agreedMonthlySalary: Number(pay), currency })}>Record hire</Button>
        </Stack>
      </Modal>
    </>
  );
}
