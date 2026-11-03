import { Badge, Button, Group, Loader, Pagination, RingProgress, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router-dom';
import { STATUS_COLOR, STATUS_LABEL } from '../lib/format.js';

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        <Title order={1} size="h2">{title}</Title>
        {subtitle && <Text c="dimmed" mt={4} maw={640}>{subtitle}</Text>}
      </div>
      {actions && <Group gap="sm">{actions}</Group>}
    </div>
  );
}

export function PageLoader({ label = 'Loading' }) {
  return (
    <div className="flex justify-center py-16" role="status" aria-label={label}>
      <Loader />
    </div>
  );
}

export function EmptyState({ title, text, actionLabel, to, onAction }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 px-6 py-12 text-center">
      <Title order={3} size="h4">{title}</Title>
      {text && <Text c="dimmed" mt={6} maw={460} mx="auto">{text}</Text>}
      {actionLabel && (
        <Button mt="md" component={to ? Link : 'button'} to={to} onClick={onAction}>{actionLabel}</Button>
      )}
    </div>
  );
}

export function StatusBadge({ status }) {
  return <Badge color={STATUS_COLOR[status] || 'gray'} variant="light" radius="sm">{STATUS_LABEL[status] || status}</Badge>;
}

export function Pager({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <Group justify="center" mt="lg">
      <Pagination value={page + 1} total={totalPages} onChange={(p) => onChange(p - 1)} size="sm" />
    </Group>
  );
}

/** Small ring showing how well a job or a candidate fits. */
export function MatchRing({ score, size = 52 }) {
  const color = score >= 75 ? 'teal' : score >= 45 ? 'signal' : 'gray';
  return (
    <RingProgress
      size={size}
      thickness={5}
      roundCaps
      sections={[{ value: Math.max(2, score), color }]}
      label={<Text ta="center" size="xs" fw={700} className="tabular">{score}</Text>}
      aria-label={`${score} percent match`}
    />
  );
}

export function ErrorNote({ message }) {
  if (!message) return null;
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
      {message}
    </div>
  );
}

export function Section({ title, children, aside }) {
  return (
    <Stack gap="sm" className="rounded-xl border border-slate-200 bg-white p-5">
      <Group justify="space-between" align="center">
        <Title order={2} size="h4">{title}</Title>
        {aside}
      </Group>
      {children}
    </Stack>
  );
}
