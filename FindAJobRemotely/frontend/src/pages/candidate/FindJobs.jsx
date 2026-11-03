import { Alert, Button, Tabs } from '@mantine/core';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import JobRow, { RowList } from '../../components/JobRow.jsx';
import JobSearch from '../../components/JobSearch.jsx';
import { EmptyState, ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';

function Recommended() {
  const [items, setItems] = useState(null);
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/candidate/recommendations', { params: { limit: 20 } }).then((r) => setItems(r.data)).catch((e) => setError(errorMessage(e)));
    api.get('/candidate/profile').then((r) => setProfile(r.data)).catch(() => {});
  }, []);

  if (error) return <ErrorNote message={error} />;
  if (!items) return <PageLoader label="Finding your matches" />;
  return (
    <div>
      {profile && !profile.profileComplete && (
        <Alert color="orange" variant="light" mb="md" title="Your matches will improve with a complete profile">
          Add a headline and your skills so we can rank jobs for you.{' '}
          <Button component={Link} to="/candidate/profile" size="xs" variant="light" ml="xs">Complete profile</Button>
        </Alert>
      )}
      {items.length === 0 ? (
        <EmptyState title="No new matches right now" text="You have applied to every open job, or none are posted yet. Check back soon or browse all jobs." />
      ) : (
        <RowList>{items.map((m) => <JobRow key={m.job.id} job={m.job} match={m} />)}</RowList>
      )}
    </div>
  );
}

export default function FindJobs() {
  return (
    <>
      <PageHeader title="Find jobs" subtitle="Jobs ranked by how well they fit your profile, or search every open role." />
      <Tabs defaultValue="matches" keepMounted={false}>
        <Tabs.List mb="md">
          <Tabs.Tab value="matches">Best matches</Tabs.Tab>
          <Tabs.Tab value="all">All jobs</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="matches"><Recommended /></Tabs.Panel>
        <Tabs.Panel value="all"><JobSearch /></Tabs.Panel>
      </Tabs>
    </>
  );
}
