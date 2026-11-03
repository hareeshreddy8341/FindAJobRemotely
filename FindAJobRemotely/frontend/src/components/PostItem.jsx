import { Badge, Group, Text } from '@mantine/core';
import { postTypeLabel, timeAgo } from '../lib/format.js';

export default function PostItem({ post, showAuthor, actions }) {
  return (
    <li className="border-b border-slate-200 px-2 py-4 last:border-b-0 sm:px-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <Group gap={8}>
            <Badge variant="light" color="signal" radius="sm">{postTypeLabel(post.type)}</Badge>
            {post.status === 'REMOVED' && <Badge color="red" variant="light">Removed by admin</Badge>}
            <Text size="xs" c="dimmed">{timeAgo(post.createdAt)}</Text>
          </Group>
          <h3 className="m-0 mt-2 font-display text-lg font-bold">{post.title}</h3>
          {showAuthor && (
            <Text size="sm" c="dimmed">{post.authorName}{post.authorHeadline ? `, ${post.authorHeadline}` : ''}</Text>
          )}
          <Text mt={6} style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }} maw={680}>{post.content}</Text>
          {post.tags?.length > 0 && (
            <Group gap={6} mt={8}>{post.tags.map((t) => <Badge key={t} variant="outline" color="gray" radius="sm" tt="none" fw={500}>{t}</Badge>)}</Group>
          )}
        </div>
        {actions}
      </div>
    </li>
  );
}
