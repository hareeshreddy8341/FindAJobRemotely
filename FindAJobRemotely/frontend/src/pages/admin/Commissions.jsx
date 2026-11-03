import { Badge, Button, Select, Table } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { fullDate, money } from '../../lib/format.js';

const COLORS = { PENDING: 'orange', PAID: 'teal', WAIVED: 'gray' };
const LABELS = { PENDING: 'To collect', PAID: 'Paid', WAIVED: 'Waived' };

export default function Commissions() {
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const load = useCallback(() => {
    api.get('/admin/commissions', { params: { page, size: 15, ...(status ? { status } : {}) } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [page, status]);
  useEffect(() => { load(); }, [load]);
  const set = async (c, next) => {
    try { await api.patch(`/admin/commissions/${c.id}`, { status: next }); load(); }
    catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
  };
  return (
    <>
      <PageHeader title="Commissions" subtitle="Created when an employer records a hire. Collect from the candidate, then mark it paid." />
      <Select aria-label="Status" w={200} mb="md" clearable placeholder="All" value={status} onChange={(v) => { setStatus(v); setPage(0); }}
        data={Object.entries(LABELS).map(([value, label]) => ({ value, label }))} />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="Nothing here" text="Commissions appear after an employer records a hire." />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <Table highlightOnHover verticalSpacing="sm">
            <Table.Thead><Table.Tr><Table.Th>Candidate</Table.Th><Table.Th>Job</Table.Th><Table.Th>Agreed pay</Table.Th><Table.Th>Commission</Table.Th><Table.Th>Status</Table.Th><Table.Th>Hired</Table.Th><Table.Th /></Table.Tr></Table.Thead>
            <Table.Tbody>
              {data.content.map((c) => (
                <Table.Tr key={c.id}>
                  <Table.Td fw={600}>{c.candidateName}</Table.Td>
                  <Table.Td>{c.jobTitle}<div className="text-xs text-slate-500">{c.companyName}</div></Table.Td>
                  <Table.Td>{money(c.agreedMonthlySalary, c.currency)}</Table.Td>
                  <Table.Td fw={600}>{money(c.amount, c.currency)} <span className="text-xs font-normal text-slate-500">({c.percent}%)</span></Table.Td>
                  <Table.Td><Badge color={COLORS[c.status]} variant="light">{LABELS[c.status]}</Badge></Table.Td>
                  <Table.Td>{fullDate(c.createdAt)}</Table.Td>
                  <Table.Td>
                    {c.status === 'PENDING' ? (
                      <div className="flex gap-1"><Button size="compact-sm" onClick={() => set(c, 'PAID')}>Mark paid</Button><Button size="compact-sm" variant="default" onClick={() => set(c, 'WAIVED')}>Waive</Button></div>
                    ) : <Button size="compact-sm" variant="subtle" color="dark" onClick={() => set(c, 'PENDING')}>Reopen</Button>}
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
