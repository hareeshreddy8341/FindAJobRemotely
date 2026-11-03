import { Alert, Button, Group, Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, MatchRing, PageHeader, PageLoader, Pager, StatusBadge } from '../../components/ui.jsx';
import { fullDate, money } from '../../lib/format.js';

export default function Applications() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.get('/candidate/applications', { params: { page, size: 10 } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [page]);
  useEffect(() => { load(); }, [load]);

  const withdraw = (a) => modals.openConfirmModal({
    title: 'Withdraw this application?',
    children: <Text size="sm">{a.companyName} will no longer consider you for {a.jobTitle}.</Text>,
    labels: { confirm: 'Withdraw', cancel: 'Keep it' },
    confirmProps: { color: 'red' },
    onConfirm: async () => {
      try {
        await api.post(`/candidate/applications/${a.id}/withdraw`);
        notifications.show({ color: 'teal', title: 'Withdrawn', message: 'Your application was withdrawn.' });
        load();
      } catch (e) { notifications.show({ color: 'red', title: 'Could not withdraw', message: errorMessage(e) }); }
    },
  });

  return (
    <>
      <PageHeader title="Applied jobs" subtitle="Follow every application from applied to hired." />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && (
        <EmptyState title="You have not applied to anything yet" text="Your applications and their progress will show up here." actionLabel="Find jobs" to="/candidate/jobs" />
      )}
      {data && data.content.length > 0 && (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">
          {data.content.map((a) => (
            <li key={a.id} className="border-b border-slate-200 px-2 py-4 last:border-b-0 sm:px-4">
              <div className="flex items-start gap-4">
                <MatchRing score={a.matchScore} />
                <div className="min-w-0 flex-1">
                  <Link to={`/jobs/${a.jobId}`} className="font-display text-lg font-bold no-underline hover:underline">{a.jobTitle}</Link>
                  <Text size="sm" fw={600}>{a.companyName}</Text>
                  <Text size="xs" c="dimmed" mt={2}>Applied {fullDate(a.appliedAt)}</Text>
                  {a.coverNote && <Text size="sm" mt={6} c="dimmed" lineClamp={2}>Your note: {a.coverNote}</Text>}
                  {a.commission && (
                    <Alert mt="sm" color="teal" variant="light" title="You were hired">
                      Agreed pay {money(a.agreedMonthlySalary, a.commission.currency)} a month. Our commission is{' '}
                      <strong>{money(a.commission.amount, a.commission.currency)}</strong> ({a.commission.percent}% of the first month).
                      We will contact you to arrange payment.
                    </Alert>
                  )}
                </div>
                <div className="flex flex-col items-end gap-2">
                  <StatusBadge status={a.status} />
                  {!['HIRED', 'REJECTED', 'WITHDRAWN'].includes(a.status) && (
                    <Button size="compact-sm" variant="subtle" color="red" onClick={() => withdraw(a)}>Withdraw</Button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      <Group mt="lg"><Text size="xs" c="dimmed">Commission applies only after a company records your hire.</Text></Group>
    </>
  );
}
