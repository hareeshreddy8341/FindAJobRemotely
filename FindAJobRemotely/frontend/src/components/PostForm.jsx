import { Button, Select, Stack, TagsInput, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { POST_TYPES } from '../lib/format.js';

export default function PostForm({ initial, onSubmit, onCancel, busy }) {
  const form = useForm({
    initialValues: initial || { type: 'PROJECT', title: '', content: '', tags: [] },
    validate: {
      title: (v) => (v.trim().length >= 3 ? null : 'Add a title of at least 3 characters'),
      content: (v) => (v.trim().length >= 10 ? null : 'Write at least 10 characters'),
    },
  });
  return (
    <form onSubmit={form.onSubmit(onSubmit)} noValidate>
      <Stack>
        <Select label="Type of post" data={POST_TYPES} {...form.getInputProps('type')} />
        <TextInput label="Title" maxLength={140} {...form.getInputProps('title')} />
        <Textarea label="What do you want employers to know?" autosize minRows={5} maxRows={14} maxLength={5000} {...form.getInputProps('content')} />
        <TagsInput label="Tags" description="Up to 8, press Enter after each" maxTags={8} {...form.getInputProps('tags')} />
        <div className="flex gap-2">
          <Button type="submit" loading={busy}>{initial ? 'Save changes' : 'Publish post'}</Button>
          {onCancel && <Button variant="default" onClick={onCancel}>Cancel</Button>}
        </div>
      </Stack>
    </form>
  );
}
