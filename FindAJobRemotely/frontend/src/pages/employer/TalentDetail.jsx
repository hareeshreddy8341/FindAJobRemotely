import { Badge, Button, Group, Text, Title } from '@mantine/core';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import PostItem from '../../components/PostItem.jsx';
import { ErrorNote, PageLoader } from '../../components/ui.jsx';
import { jobTypeLabel, levelLabel } from '../../lib/format.js';

const Ext = ({ href, children }) => (href ? <a href={href} target="_blank" rel="noopener noreferrer nofollow" className="font-semibold text-signal">{children}</a> : null);

export default function TalentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api.get(`/employer/talent/${id}`).then((r) => setD(r.data)).catch((e) => setError(errorMessage(e, 'This candidate is not available.'))); }, [id]);
  if (error) return <ErrorNote message={error} />;
  if (!d) return <PageLoader />;
  const p = d.profile;
  return (
    <div className="max-w-3xl">
      <Button variant="subtle" color="dark" px={0} onClick={() => navigate(-1)}>Back</Button>
      <Title order={1} mt="xs">{p.fullName}</Title>
      <Text size="lg" fw={500}>{p.headline}</Text>
      <Group gap={8} mt="xs">
        {p.experienceLevel && <Badge variant="light" color="gray" radius="sm">{levelLabel(p.experienceLevel)}</Badge>}
        {p.yearsOfExperience != null && <Text size="sm" c="dimmed">{p.yearsOfExperience} years of experience</Text>}
        {p.country && <Text size="sm" c="dimmed">· {p.country}</Text>}
        {p.timezone && <Text size="sm" c="dimmed">· {p.timezone}</Text>}
      </Group>
      {p.about && <Text mt="md" style={{ whiteSpace: 'pre-wrap', lineHeight: 1.65 }}>{p.about}</Text>}
      <Title order={2} size="h4" mt="xl">Skills</Title>
      <Group gap={6} mt="xs">{p.skills.map((s) => <Badge key={s} variant="outline" color="signal" radius="sm" tt="none" fw={500}>{s}</Badge>)}</Group>
      {p.preferredJobTypes?.length > 0 && <Text mt="md" size="sm">Looking for: {p.preferredJobTypes.map(jobTypeLabel).join(', ')}</Text>}
      {p.education && <Text size="sm" mt={4}>Education: {p.education}</Text>}
      <Group gap="md" mt="sm"><Ext href={p.githubUrl}>GitHub</Ext><Ext href={p.linkedinUrl}>LinkedIn</Ext><Ext href={p.portfolioUrl}>Portfolio</Ext></Group>
      <Text size="sm" c="dimmed" mt="md">Contact details are shared only if this candidate applies to one of your jobs. <Link to="/employer/post-job" className="text-signal">Post a job</Link> to invite applications.</Text>

      <Title order={2} size="h4" mt="xl" mb="xs">Posts</Title>
      {d.posts.length === 0 ? <Text c="dimmed">No posts yet.</Text> : (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">{d.posts.map((x) => <PostItem key={x.id} post={x} />)}</ul>
      )}
    </div>
  );
}
