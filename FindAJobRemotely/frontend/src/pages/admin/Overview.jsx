import { Text } from '@mantine/core';
import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';

const TILES = [
  ['candidates', 'Candidates'], ['employers', 'Employers'], ['openJobs', 'Open jobs'],
  ['applications', 'Applications'], ['hires', 'Hires recorded'], ['pendingCommissions', 'Commissions to collect'],
  ['suspendedUsers', 'Suspended accounts'],
];

export default function Overview() {
  const [s, setS] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api.get('/admin/stats').then((r) => setS(r.data)).catch((e) => setError(errorMessage(e))); }, []);
  return (
    <>
      <PageHeader title="Overview" subtitle="A snapshot of the platform." />
      <ErrorNote message={error} />
      {!s && !error && <PageLoader />}
      {s && (
        <dl className="m-0 grid gap-x-8 gap-y-6 sm:grid-cols-2 lg:grid-cols-3">
          {TILES.map(([k, label]) => (
            <div key={k} className="border-t-2 border-ink pt-2">
              <dd className="m-0 font-display text-4xl font-bold tabular">{s[k]}</dd>
              <dt><Text size="sm" c="dimmed">{label}</Text></dt>
            </div>
          ))}
        </dl>
      )}
    </>
  );
}
