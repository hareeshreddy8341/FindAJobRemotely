import { Select, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { RowList } from '../../components/JobRow.jsx';
import TalentRow from '../../components/TalentRow.jsx';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';
import { JOB_TYPES, LEVELS } from '../../lib/format.js';

export default function Talent() {
  const [f, setF] = useState({ q: '', skill: '', level: null, type: null });
  const [debounced, setDebounced] = useState(f);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { const t = setTimeout(() => { setDebounced(f); setPage(0); }, 350); return () => clearTimeout(t); }, [f]);
  useEffect(() => {
    let cancelled = false;
    const params = { page, size: 12 };
    Object.entries(debounced).forEach(([k, v]) => { if (v) params[k] = v; });
    setError('');
    api.get('/employer/talent', { params }).then((r) => !cancelled && setData(r.data)).catch((e) => !cancelled && setError(errorMessage(e)));
    return () => { cancelled = true; };
  }, [debounced, page]);

  const set = (k) => (v) => setF((s) => ({ ...s, [k]: typeof v === 'string' || v === null ? v : v.currentTarget.value }));

  return (
    <>
      <PageHeader title="Find talent" subtitle="Search public candidate profiles. Contact details unlock when a candidate applies to one of your jobs." />
      <form role="search" onSubmit={(e) => e.preventDefault()} className="mb-5 grid gap-3 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-4">
        <TextInput aria-label="Search talent" placeholder="Headline, role or skill" leftSection={<IconSearch size={16} />} value={f.q} onChange={set('q')} maxLength={80} />
        <TextInput aria-label="Exact skill" placeholder="Exact skill, e.g. Python" value={f.skill} onChange={set('skill')} maxLength={40} />
        <Select aria-label="Experience level" placeholder="Any level" clearable data={LEVELS} value={f.level} onChange={set('level')} />
        <Select aria-label="Looking for" placeholder="Any kind of work" clearable data={JOB_TYPES} value={f.type} onChange={set('type')} />
      </form>
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No candidates match" text="Try fewer filters or a broader keyword." />}
      {data && data.content.length > 0 && <RowList>{data.content.map((c) => <TalentRow key={c.userId} candidate={c} />)}</RowList>}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  );
}
