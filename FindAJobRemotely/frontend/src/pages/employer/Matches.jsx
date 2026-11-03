import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@mantine/core';
import api, { errorMessage } from '../../api/client.js';
import TalentRow from '../../components/TalentRow.jsx';
import { EmptyState, ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';
import { RowList } from '../../components/JobRow.jsx';

export default function Matches() {
  const { id } = useParams();
  const [items, setItems] = useState(null);
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    api.get(`/jobs/${id}`).then((r) => setJob(r.data)).catch(() => {});
    api.get(`/employer/jobs/${id}/matches`, { params: { limit: 25 } }).then((r) => setItems(r.data)).catch((e) => setError(errorMessage(e)));
  }, [id]);
  return (
    <>
      <PageHeader title="Best-fit candidates" subtitle={job ? `Public profiles ranked for ${job.title}. Highlighted skills match your requirements.` : ''}
        actions={<Button component={Link} to="/employer/jobs" variant="subtle" color="dark">Back to jobs</Button>} />
      <ErrorNote message={error} />
      {!items && !error && <PageLoader label="Ranking candidates" />}
      {items && items.length === 0 && <EmptyState title="No public candidates fit yet" text="Candidates with public profiles will appear here as they join." />}
      {items && items.length > 0 && <RowList>{items.map((m) => <TalentRow key={m.candidate.userId} candidate={m.candidate} match={m} />)}</RowList>}
    </>
  );
}
