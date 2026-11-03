import { TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import PostItem from '../../components/PostItem.jsx';
import { EmptyState, ErrorNote, PageHeader, PageLoader, Pager } from '../../components/ui.jsx';

export default function CandidatePosts() {
  const [q, setQ] = useState('');
  const [dq, setDq] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { const t = setTimeout(() => { setDq(q); setPage(0); }, 350); return () => clearTimeout(t); }, [q]);
  useEffect(() => {
    let cancelled = false;
    setError('');
    api.get('/employer/posts', { params: { page, size: 10, ...(dq ? { q: dq } : {}) } }).then((r) => !cancelled && setData(r.data)).catch((e) => !cancelled && setError(errorMessage(e)));
    return () => { cancelled = true; };
  }, [dq, page]);
  return (
    <>
      <PageHeader title="Candidate posts" subtitle="Projects, achievements and availability notes from candidates with public profiles." />
      <div className="mb-4 max-w-md"><TextInput aria-label="Search posts" placeholder="Search posts" leftSection={<IconSearch size={16} />} value={q} onChange={(e) => setQ(e.currentTarget.value)} maxLength={80} /></div>
      <ErrorNote message={error} />
      {!data && !error && <PageLoader />}
      {data && data.content.length === 0 && <EmptyState title="No posts match" text="Try another keyword." />}
      {data && data.content.length > 0 && (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">
          {data.content.map((p) => (
            <PostItem key={p.id} post={p} showAuthor actions={<Link to={`/employer/talent/${p.authorId}`} className="shrink-0 text-sm font-semibold text-signal">View profile</Link>} />
          ))}
        </ul>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  );
}
