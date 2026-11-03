import { Table } from '@mantine/core';
import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { fullDate } from '../../lib/format.js';

export default function Audit() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api.get('/admin/audit', { params: { page, size: 25 } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e))); }, [page]);
  return (
    <>
      <PageHeader title="Audit log" subtitle="Sign-ins, lockouts, administrator actions and hires, newest first." />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No events yet" />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <Table verticalSpacing="xs" fz="sm">
            <Table.Thead><Table.Tr><Table.Th>When</Table.Th><Table.Th>Who</Table.Th><Table.Th>Action</Table.Th><Table.Th>Details</Table.Th><Table.Th>IP</Table.Th></Table.Tr></Table.Thead>
            <Table.Tbody>
              {data.content.map((a) => (
                <Table.Tr key={a.id}>
                  <Table.Td className="whitespace-nowrap">{fullDate(a.createdAt)}</Table.Td>
                  <Table.Td>{a.actorEmail || 'System'}</Table.Td>
                  <Table.Td fw={600}>{a.action}</Table.Td>
                  <Table.Td>{a.details}</Table.Td>
                  <Table.Td>{a.ip}</Table.Td>
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
