import { Badge, Group, Text } from '@mantine/core';
import { IconRosetteDiscountCheckFilled } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { MatchRing } from './ui.jsx';
import { jobTypeLabel, levelLabel, payRange, timeAgo } from '../lib/format.js';

/** One job as a row in a divided list. `match` is optional extra data for recommendations. */
export default function JobRow({ job, match, trailing, to }) {
  return (
    <li className="row-link border-b border-slate-200 last:border-b-0">
      <div className="flex items-start gap-4 px-2 py-4 sm:px-4">
        {match && <MatchRing score={match.score} />}
        <div className="min-w-0 flex-1">
          <Link to={to || `/jobs/${job.id}`} className="font-display text-lg font-bold no-underline hover:underline">
            {job.title}
          </Link>
          <Group gap={6} mt={2}>
            <Text size="sm" fw={600}>{job.companyName}</Text>
            {job.companyVerified && (
              <IconRosetteDiscountCheckFilled size={16} color="#2F4BFF" aria-label="Verified company" />
            )}
            <Text size="sm" c="dimmed">· {job.remoteRegion}</Text>
          </Group>
          <Group gap={8} mt={8}>
            <Badge variant="light" color="gray" radius="sm">{jobTypeLabel(job.type)}</Badge>
            <Badge variant="light" color="gray" radius="sm">{levelLabel(job.experienceLevel)}</Badge>
            <Text size="sm" c="dimmed">{payRange(job)}</Text>
          </Group>
          <Group gap={6} mt={8}>
            {(job.skills || []).slice(0, 6).map((s) => (
              <Badge key={s} variant="outline" color="signal" radius="sm" tt="none" fw={500}>{s}</Badge>
            ))}
          </Group>
          {match && match.missingSkills?.length > 0 && (
            <Text size="xs" c="dimmed" mt={8}>Skills to add: {match.missingSkills.slice(0, 4).join(', ')}</Text>
          )}
        </div>
        <div className="flex shrink-0 flex-col items-end gap-2">
          <Text size="xs" c="dimmed">{timeAgo(job.createdAt)}</Text>
          {trailing}
        </div>
      </div>
    </li>
  );
}

export const RowList = ({ children }) => (
  <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">{children}</ul>
);
