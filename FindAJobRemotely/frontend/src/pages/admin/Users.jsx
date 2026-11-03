import { Badge, Button, Drawer, Group, Select, Table, Text, TextInput } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { IconSearch } from '@tabler/icons-react';
import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { fullDate, levelLabel } from '../../lib/format.js';

export default function Users() {
  const [f, setF] = useState({ q: '', role: null, status: null });
  const [d, setD] = useState(f);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);

  useEffect(() => { const t = setTimeout(() => { setD(f); setPage(0); }, 350); return () => clearTimeout(t); }, [f]);
  const load = useCallback(() => {
    const params = { page, size: 15 };
    Object.entries(d).forEach(([k, v]) => { if (v) params[k] = v; });
    api.get('/admin/users', { params }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [d, page]);
  useEffect(() => { load(); }, [load]);

  const open = (u) => api.get(`/admin/users/${u.id}`).then((r) => setDetail(r.data)).catch((e) => notifications.show({ color: 'red', title: 'Could not load', message: errorMessage(e) }));

  const toggle = (u) => {
    const suspend = u.status === 'ACTIVE';
    modals.openConfirmModal({
      title: suspend ? `Suspend ${u.fullName}?` : `Reactivate ${u.fullName}?`,
      children: <Text size="sm">{suspend ? 'They are signed out immediately, hidden from search, and an employer’s open jobs are closed.' : 'They will be able to sign in again.'}</Text>,
      labels: { confirm: suspend ? 'Suspend' : 'Reactivate', cancel: 'Cancel' }, confirmProps: { color: suspend ? 'red' : 'teal' },
      onConfirm: async () => {
        try { await api.patch(`/admin/users/${u.id}/status`, { status: suspend ? 'SUSPENDED' : 'ACTIVE' }); load(); setDetail(null); }
        catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
      },
    });
  };
  const remove = (u) => modals.openConfirmModal({
    title: `Delete ${u.fullName}?`, children: <Text size="sm">This permanently deletes the account and everything they uploaded. It cannot be undone.</Text>,
    labels: { confirm: 'Delete permanently', cancel: 'Cancel' }, confirmProps: { color: 'red' },
    onConfirm: async () => {
      try { await api.delete(`/admin/users/${u.id}`); load(); setDetail(null); }
      catch (e) { notifications.show({ color: 'red', title: 'Could not delete', message: errorMessage(e) }); }
    },
  });
  const verify = async (employerId, verified) => {
    try { await api.patch(`/admin/employers/${employerId}/verify`, { verified }); const r = await api.get(`/admin/users/${employerId}`); setDetail(r.data); }
    catch (e) { notifications.show({ color: 'red', title: 'Could not update', message: errorMessage(e) }); }
  };

  return (
    <>
      <PageHeader title="Users" subtitle="Candidates and employers. You can see account details and public profile data only." />
      <div className="mb-4 grid gap-3 md:grid-cols-3">
        <TextInput aria-label="Search users" placeholder="Name or email" leftSection={<IconSearch size={16} />} value={f.q} onChange={(e) => setF({ ...f, q: e.currentTarget.value })} maxLength={80} />
        <Select aria-label="Role" placeholder="All roles" clearable data={[{ value: 'CANDIDATE', label: 'Candidates' }, { value: 'EMPLOYER', label: 'Employers' }]} value={f.role} onChange={(v) => setF({ ...f, role: v })} />
        <Select aria-label="Status" placeholder="Any status" clearable data={[{ value: 'ACTIVE', label: 'Active' }, { value: 'SUSPENDED', label: 'Suspended' }]} value={f.status} onChange={(v) => setF({ ...f, status: v })} />
      </div>
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No users match" text="Adjust the filters." />}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <Table highlightOnHover verticalSpacing="sm">
            <Table.Thead><Table.Tr><Table.Th>Name</Table.Th><Table.Th>Email</Table.Th><Table.Th>Role</Table.Th><Table.Th>Status</Table.Th><Table.Th>Joined</Table.Th><Table.Th /></Table.Tr></Table.Thead>
            <Table.Tbody>
              {data.content.map((u) => (
                <Table.Tr key={u.id}>
                  <Table.Td fw={600}>{u.fullName}</Table.Td>
                  <Table.Td>{u.email}</Table.Td>
                  <Table.Td>{u.role === 'CANDIDATE' ? 'Candidate' : 'Employer'}</Table.Td>
                  <Table.Td><Badge color={u.status === 'ACTIVE' ? 'teal' : 'red'} variant="light">{u.status === 'ACTIVE' ? 'Active' : 'Suspended'}</Badge></Table.Td>
                  <Table.Td>{fullDate(u.createdAt)}</Table.Td>
                  <Table.Td><Button size="compact-sm" variant="light" onClick={() => open(u)}>Manage</Button></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <Drawer opened={Boolean(detail)} onClose={() => setDetail(null)} position="right" size="md" title={detail?.user.fullName}>
        {detail && (
          <div className="space-y-4">
            <div>
              <Text size="sm">{detail.user.email}</Text>
              <Text size="sm" c="dimmed">Joined {fullDate(detail.user.createdAt)}{detail.user.lastLoginAt ? ` · last sign-in ${fullDate(detail.user.lastLoginAt)}` : ''}</Text>
            </div>
            {detail.candidate && (
              <div>
                <Text fw={600}>{detail.candidate.headline || 'No headline yet'}</Text>
                <Text size="sm" c="dimmed">{detail.candidate.experienceLevel ? levelLabel(detail.candidate.experienceLevel) : ''} {detail.candidate.country ? `· ${detail.candidate.country}` : ''}</Text>
                <Group gap={6} mt={6}>{(detail.candidate.skills || []).map((s) => <Badge key={s} variant="outline" color="gray" tt="none" fw={500}>{s}</Badge>)}</Group>
                <Text size="sm" mt="sm">{detail.applications} applications · {detail.posts} posts</Text>
              </div>
            )}
            {detail.employer && (
              <div>
                <Text fw={600}>{detail.employer.companyName}</Text>
                <Text size="sm" c="dimmed">{detail.employer.industry} {detail.employer.country ? `· ${detail.employer.country}` : ''}</Text>
                {detail.employer.website && <Text size="sm"><a href={detail.employer.website} target="_blank" rel="noopener noreferrer nofollow">{detail.employer.website}</a></Text>}
                <Text size="sm" mt="sm">{detail.jobs} jobs posted</Text>
                <Button mt="sm" size="compact-md" variant={detail.employer.verified ? 'default' : 'filled'} onClick={() => verify(detail.user.id, !detail.employer.verified)}>
                  {detail.employer.verified ? 'Remove verified badge' : 'Mark as verified'}
                </Button>
              </div>
            )}
            <Text size="xs" c="dimmed">Phone numbers, salary expectations and resumes are private and not shown here.</Text>
            <Group>
              <Button color={detail.user.status === 'ACTIVE' ? 'orange' : 'teal'} variant="light" onClick={() => toggle(detail.user)}>{detail.user.status === 'ACTIVE' ? 'Suspend' : 'Reactivate'}</Button>
              <Button color="red" variant="light" onClick={() => remove(detail.user)}>Delete account</Button>
            </Group>
          </div>
        )}
      </Drawer>
    </>
  );
}
