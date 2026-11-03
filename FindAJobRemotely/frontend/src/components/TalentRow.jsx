import { Badge, Group, Text } from '@mantine/core';
import { Link } from 'react-router-dom';
import { MatchRing } from './ui.jsx';
import { levelLabel } from '../lib/format.js';

/** A candidate's public profile as a list row. Only public fields are ever passed in. */
export default function TalentRow({ candidate, match, to }) {
  return (
    <li className="row-link border-b border-slate-200 last:border-b-0">
      <div className="flex items-start gap-4 px-2 py-4 sm:px-4">
        {match ? (
          <MatchRing score={match.score} />
        ) : (
          <div
            aria-hidden="true"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-ink font-display text-lg font-bold text-white"
          >
            {candidate.fullName.charAt(0).toUpperCase()}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <Link to={to || `/employer/talent/${candidate.userId}`} className="font-display text-lg font-bold no-underline hover:underline">
            {candidate.fullName}
          </Link>
          <Text size="sm" fw={500}>{candidate.headline}</Text>
          <Group gap={8} mt={4}>
            {candidate.experienceLevel && <Text size="sm" c="dimmed">{levelLabel(candidate.experienceLevel)}</Text>}
            {candidate.country && <Text size="sm" c="dimmed">· {candidate.country}</Text>}
            {candidate.timezone && <Text size="sm" c="dimmed">· {candidate.timezone}</Text>}
          </Group>
          <Group gap={6} mt={8}>
            {(candidate.skills || []).slice(0, 8).map((s) => (
              <Badge
                key={s}
                variant={match?.matchedSkills?.some((m) => m.toLowerCase() === s.toLowerCase()) ? 'filled' : 'outline'}
                color="signal" radius="sm" tt="none" fw={500}
              >
                {s}
              </Badge>
            ))}
          </Group>
        </div>
      </div>
    </li>
  );
}
