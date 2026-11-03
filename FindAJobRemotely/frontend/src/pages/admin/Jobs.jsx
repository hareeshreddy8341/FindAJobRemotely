import { Badge, Button, Table } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { jobTypeLabel, timeAgo } from '../../lib/format.js';

const COLORS = { OPEN: 'teal', CLOSED: 'gray', REMOVED: 'red' };

export default function AdminJobs() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const load = useCallback(() => { api.get('/admin/jobs', { params: { page, size: 15 } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e))); }, [page]);
  useEffect(() => { load(); }, [load]);
  const set = async (j, status) => {
    try { await api.patch(`/admin/jobs/${j.id}/status`, { status }); load(); }
    catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
  };
  return (
    <>
      <PageHeader title="Jobs" subtitle="Remove jobs that break the rules. The employer is told the job was removed by an administrator." />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No jobs yet" />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <Table highlightOnHover verticalSpacing="sm">
            <Table.Thead><Table.Tr><Table.Th>Job</Table.Th><Table.Th>Company</Table.Th><Table.Th>Type</Table.Th><Table.Th>Applicants</Table.Th><Table.Th>Status</Table.Th><Table.Th>Posted</Table.Th><Table.Th /></Table.Tr></Table.Thead>
            <Table.Tbody>
              {data.content.map((j) => (
                <Table.Tr key={j.id}>
                  <Table.Td fw={600}><Link to={`/jobs/${j.id}`}>{j.title}</Link></Table.Td>
                  <Table.Td>{j.companyName}</Table.Td>
                  <Table.Td>{jobTypeLabel(j.type)}</Table.Td>
                  <Table.Td>{j.applicantCount ?? 0}</Table.Td>
                  <Table.Td><Badge color={COLORS[j.status]} variant="light">{j.status === 'OPEN' ? 'Open' : j.status === 'CLOSED' ? 'Closed' : 'Removed'}</Badge></Table.Td>
                  <Table.Td>{timeAgo(j.createdAt)}</Table.Td>
                  <Table.Td>
                    {j.status === 'REMOVED'
                      ? <Button size="compact-sm" variant="light" onClick={() => set(j, 'CLOSED')}>Restore</Button>
                      : <Button size="compact-sm" variant="light" color="red" onClick={() => set(j, 'REMOVED')}>Remove</Button>}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  );
}
