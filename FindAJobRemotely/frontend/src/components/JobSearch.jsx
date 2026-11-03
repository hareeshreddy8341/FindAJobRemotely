import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import api, { errorMessage } from '../api/client.js';
import JobFilters from './JobFilters.jsx';
import JobRow, { RowList } from './JobRow.jsx';
import { EmptyState, ErrorNote, PageLoader, Pager } from './ui.jsx';
import { resetFilters, setFilters } from '../store/jobsSlice.js';

/** Search and filter open jobs. Filters live in Redux so they survive navigation. */
export default function JobSearch() {
  const dispatch = useDispatch();
  const filters = useSelector((s) => s.jobs.filters);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    setError('');
    const params = { page: filters.page, size: 10 };
    ['q', 'type', 'level', 'skill'].forEach((k) => { if (filters[k]) params[k] = filters[k]; });
    api.get('/jobs', { params })
      .then((r) => { if (!cancelled) setData(r.data); })
      .catch((e) => { if (!cancelled) setError(errorMessage(e)); });
    return () => { cancelled = true; };
  }, [filters]);

  return (
    <div>
      <JobFilters filters={filters} onChange={(p) => dispatch(setFilters(p))} onReset={() => dispatch(resetFilters())} />
      <ErrorNote message={error} />
      {!data && !error && <PageLoader label="Loading jobs" />}
      {data && data.content.length === 0 && (
        <EmptyState title="No jobs match these filters" text="Try a different keyword or clear the filters to see every open role."
          actionLabel="Clear filters" onAction={() => dispatch(resetFilters())} />
      )}
      {data && data.content.length > 0 && (
        <>
          <p className="mb-2 text-sm text-slate-600" aria-live="polite">{data.totalElements} open {data.totalElements === 1 ? 'job' : 'jobs'}</p>
          <RowList>{data.content.map((j) => <JobRow key={j.id} job={j} />)}</RowList>
          <Pager page={data.page} totalPages={data.totalPages} onChange={(p) => dispatch(setFilters({ page: p }))} />
        </>
      )}
    </div>
  );
}
