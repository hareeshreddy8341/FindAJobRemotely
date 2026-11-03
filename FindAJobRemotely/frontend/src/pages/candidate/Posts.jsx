import { Button, Modal } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import PostForm from '../../components/PostForm.jsx';
import PostItem from '../../components/PostItem.jsx';
import { EmptyState, ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';

export default function Posts() {
  const [posts, setPosts] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [opened, { open, close }] = useDisclosure(false);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    api.get('/candidate/posts').then((r) => setPosts(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);
  useEffect(() => { load(); }, [load]);

  const startNew = () => { setEditing(null); open(); };
  const startEdit = (p) => { setEditing(p); open(); };

  const save = async (values) => {
    setBusy(true);
    try {
      if (editing) await api.put(`/candidate/posts/${editing.id}`, values);
      else await api.post('/candidate/posts', values);
      notifications.show({ color: 'teal', title: editing ? 'Saved' : 'Published', message: editing ? 'Your post was updated.' : 'Employers can now see your post.' });
      close();
      load();
    } catch (e) {
      notifications.show({ color: 'red', title: 'Could not save', message: errorMessage(e) });
    } finally { setBusy(false); }
  };

  const remove = (p) => modals.openConfirmModal({
    title: 'Delete this post?', children: <p className="m-0 text-sm">“{p.title}” will be removed for good.</p>,
    labels: { confirm: 'Delete', cancel: 'Keep it' }, confirmProps: { color: 'red' },
    onConfirm: async () => {
      try { await api.delete(`/candidate/posts/${p.id}`); notifications.show({ color: 'teal', title: 'Deleted', message: 'Post deleted.' }); load(); }
      catch (e) { notifications.show({ color: 'red', title: 'Could not delete', message: errorMessage(e) }); }
    },
  });

  return (
    <>
      <PageHeader title="My posts" subtitle="Show employers what you have built, achieved or when you are available. Posts appear to employers only while your profile is public."
        actions={<Button onClick={startNew}>New post</Button>} />
      <ErrorNote message={error} />
      {!posts && !error && <PageLoader />}
      {posts && posts.length === 0 && (
        <EmptyState title="You have not posted anything yet" text="A project write-up or an availability note helps employers find you." actionLabel="Write your first post" onAction={startNew} />
      )}
      {posts && posts.length > 0 && (
        <ul className="m-0 list-none rounded-xl border border-slate-200 bg-white/70 p-0">
          {posts.map((p) => (
            <PostItem key={p.id} post={p} actions={
              <div className="flex shrink-0 gap-1">
                <Button size="compact-sm" variant="subtle" onClick={() => startEdit(p)} disabled={p.status === 'REMOVED'}>Edit</Button>
                <Button size="compact-sm" variant="subtle" color="red" onClick={() => remove(p)}>Delete</Button>
              </div>} />
          ))}
        </ul>
      )}
      <Modal opened={opened} onClose={close} title={editing ? 'Edit post' : 'New post'} size="lg">
        <PostForm key={editing?.id || 'new'} initial={editing ? { type: editing.type, title: editing.title, content: editing.content, tags: editing.tags || [] } : null}
          onSubmit={save} onCancel={close} busy={busy} />
      </Modal>
    </>
  );
}
