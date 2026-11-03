import { Badge, Button, Table } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { postTypeLabel, timeAgo } from '../../lib/format.js';

export default function AdminPosts() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const load = useCallback(() => { api.get('/admin/posts', { params: { page, size: 15 } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e))); }, [page]);
  useEffect(() => { load(); }, [load]);
  const set = async (p, status) => {
    try { await api.patch(`/admin/posts/${p.id}/status`, { status }); load(); }
    catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
  };
  return (
    <>
      <PageHeader title="Candidate posts" subtitle="Hide posts that break the rules. Hidden posts disappear from employer search." />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No posts yet" />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <Table highlightOnHover verticalSpacing="sm">
            <Table.Thead><Table.Tr><Table.Th>Title</Table.Th><Table.Th>Author</Table.Th><Table.Th>Type</Table.Th><Table.Th>Status</Table.Th><Table.Th>Posted</Table.Th><Table.Th /></Table.Tr></Table.Thead>
            <Table.Tbody>
              {data.content.map((p) => (
                <Table.Tr key={p.id}>
                  <Table.Td fw={600} maw={280}>{p.title}</Table.Td>
                  <Table.Td>{p.authorName}</Table.Td>
                  <Table.Td>{postTypeLabel(p.type)}</Table.Td>
                  <Table.Td><Badge color={p.status === 'VISIBLE' ? 'teal' : 'red'} variant="light">{p.status === 'VISIBLE' ? 'Visible' : 'Hidden'}</Badge></Table.Td>
                  <Table.Td>{timeAgo(p.createdAt)}</Table.Td>
                  <Table.Td>
                    {p.status === 'VISIBLE'
                      ? <Button size="compact-sm" variant="light" color="red" onClick={() => set(p, 'REMOVED')}>Hide</Button>
                      : <Button size="compact-sm" variant="light" onClick={() => set(p, 'VISIBLE')}>Restore</Button>}
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
