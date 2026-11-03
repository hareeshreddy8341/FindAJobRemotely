import { Button, PasswordInput, SegmentedControl, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { AuthShell, FormError } from './AuthForm.jsx';
import { errorMessage } from '../api/client.js';
import { register } from '../store/authThunks.js';
import { roleHome } from '../lib/format.js';

/** Mirrors the server rules so people see problems before sending the form. The server still enforces them. */
export function passwordProblem(pw, email) {
  if (!pw || pw.length < 10) return 'Use at least 10 characters';
  if (pw.length > 64) return 'Use at most 64 characters';
  if (!/[A-Za-z]/.test(pw) || !/\d/.test(pw)) return 'Include at least one letter and one number';
  const local = (email || '').split('@')[0].toLowerCase();
  if (local.length >= 4 && pw.toLowerCase().includes(local)) return 'Do not include your email name in the password';
  return null;
}

export default function Register() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const form = useForm({
    initialValues: {
      role: params.get('role') === 'EMPLOYER' ? 'EMPLOYER' : 'CANDIDATE',
      fullName: '', companyName: '', email: '', password: '',
    },
    validate: {
      fullName: (v) => (v.trim().length >= 2 ? null : 'Enter your full name'),
      companyName: (v, vals) => (vals.role === 'EMPLOYER' && v.trim().length < 2 ? 'Enter your company name' : null),
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : 'Enter a valid email address'),
      password: (v, vals) => passwordProblem(v, vals.email),
    },
  });
  const employer = form.values.role === 'EMPLOYER';

  const submit = async (values) => {
    setBusy(true);
    setError('');
    try {
      const body = { ...values, companyName: values.role === 'EMPLOYER' ? values.companyName : undefined };
      const user = await dispatch(register(body));
      navigate(user.role === 'EMPLOYER' ? '/employer/profile' : '/candidate/profile', { replace: true, state: { welcome: true } });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Create your profile" subtitle="Free for candidates. Startups pay only when they hire.">
      <FormError message={error} />
      <form onSubmit={form.onSubmit(submit)} noValidate>
        <Stack>
          <div>
            <Text size="sm" fw={500} mb={4} id="role-label">I am a</Text>
            <SegmentedControl fullWidth aria-labelledby="role-label" data={[
              { value: 'CANDIDATE', label: 'Candidate' },
              { value: 'EMPLOYER', label: 'Employer' },
            ]} {...form.getInputProps('role')} />
          </div>
          <TextInput label="Full name" autoComplete="name" {...form.getInputProps('fullName')} />
          {employer && <TextInput label="Company name" autoComplete="organization" {...form.getInputProps('companyName')} />}
          <TextInput label="Email" type="email" autoComplete="email" {...form.getInputProps('email')} />
          <PasswordInput label="Password" description="At least 10 characters, with a letter and a number"
            autoComplete="new-password" {...form.getInputProps('password')} />
          <Button type="submit" loading={busy} fullWidth size="md">Create profile</Button>
        </Stack>
      </form>
      <p className="mt-5 text-sm text-slate-600">
        Already registered? <Link to="/login" className="font-semibold text-signal">Sign in</Link>
      </p>
    </AuthShell>
  );
}
