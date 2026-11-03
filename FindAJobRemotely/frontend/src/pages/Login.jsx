import { Button, PasswordInput, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthShell, FormError } from './AuthForm.jsx';
import { errorMessage } from '../api/client.js';
import { login } from '../store/authThunks.js';
import { roleHome } from '../lib/format.js';

export default function Login({ admin = false }) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const form = useForm({
    initialValues: { email: '', password: '' },
    validate: {
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : 'Enter a valid email address'),
      password: (v) => (v ? null : 'Enter your password'),
    },
  });

  const submit = async (values) => {
    setBusy(true);
    setError('');
    try {
      const user = await dispatch(login(values, admin));
      const from = location.state?.from;
      navigate(from && from.startsWith('/') && !from.startsWith('//') ? from : roleHome(user.role), { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell
      dark={admin}
      title={admin ? 'Admin sign in' : 'Sign in'}
      subtitle={admin ? 'This portal is for platform administrators only.' : 'Welcome back. Sign in to continue.'}
    >
      <FormError message={error} />
      <form onSubmit={form.onSubmit(submit)} noValidate>
        <Stack>
          <TextInput label="Email" type="email" autoComplete="username" {...form.getInputProps('email')} />
          <PasswordInput label="Password" autoComplete="current-password" {...form.getInputProps('password')} />
          <Button type="submit" loading={busy} fullWidth size="md" color={admin ? 'signal' : undefined}>Sign in</Button>
        </Stack>
      </form>
      {!admin && (
        <p className="mt-5 text-sm text-slate-600">
          New here? <Link to="/register" className="font-semibold text-signal">Create a profile</Link>
        </p>
      )}
    </AuthShell>
  );
}
