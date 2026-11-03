import { Button, Modal, PasswordInput, Stack, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useDisclosure } from '@mantine/hooks';
import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage } from '../api/client.js';
import { ErrorNote, PageHeader, Section } from '../components/ui.jsx';
import { passwordProblem } from './Register.jsx';
import { sessionStarted, signedOut } from '../store/authSlice.js';

export default function Account() {
  const user = useSelector((s) => s.auth.user);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [opened, { open, close }] = useDisclosure(false);
  const [error, setError] = useState('');
  const [delError, setDelError] = useState('');

  const pw = useForm({
    initialValues: { currentPassword: '', newPassword: '' },
    validate: {
      currentPassword: (v) => (v ? null : 'Enter your current password'),
      newPassword: (v) => passwordProblem(v, user.email),
    },
  });
  const del = useForm({ initialValues: { password: '' }, validate: { password: (v) => (v ? null : 'Enter your password to confirm') } });

  const changePassword = async (values) => {
    setError('');
    try {
      const { data } = await api.post('/auth/change-password', values);
      dispatch(sessionStarted(data));
      pw.reset();
      notifications.show({ color: 'teal', title: 'Password changed', message: 'You were signed out on all your other devices.' });
    } catch (e) { setError(errorMessage(e)); }
  };

  const deleteAccount = async (values) => {
    setDelError('');
    try {
      await api.delete('/auth/account', { data: values });
      dispatch(signedOut());
      navigate('/');
      notifications.show({ color: 'teal', title: 'Account deleted', message: 'Your profile and data were removed.' });
    } catch (e) { setDelError(errorMessage(e)); }
  };

  return (
    <div className="max-w-xl space-y-6">
      <PageHeader title="Account and security" subtitle={`Signed in as ${user.email}`} />
      <Section title="Change password">
        <ErrorNote message={error} />
        <form onSubmit={pw.onSubmit(changePassword)} noValidate>
          <Stack>
            <PasswordInput label="Current password" autoComplete="current-password" {...pw.getInputProps('currentPassword')} />
            <PasswordInput label="New password" description="At least 10 characters, with a letter and a number" autoComplete="new-password" {...pw.getInputProps('newPassword')} />
            <Button type="submit" w="fit-content">Change password</Button>
          </Stack>
        </form>
      </Section>
      <Section title="Delete account">
        <Text size="sm" c="dimmed">This permanently removes your profile, resume, posts and applications. It cannot be undone.</Text>
        <Button color="red" variant="light" w="fit-content" onClick={open}>Delete my account</Button>
      </Section>
      <Modal opened={opened} onClose={close} title="Delete your account?">
        <ErrorNote message={delError} />
        <form onSubmit={del.onSubmit(deleteAccount)} noValidate>
          <Stack>
            <Text size="sm">Enter your password to confirm.</Text>
            <PasswordInput label="Password" autoComplete="current-password" {...del.getInputProps('password')} />
            <Button type="submit" color="red">Delete account permanently</Button>
          </Stack>
        </form>
      </Modal>
    </div>
  );
}
