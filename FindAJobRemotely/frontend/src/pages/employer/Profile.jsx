import { Alert, Badge, Button, Group, NumberInput, Select, Text, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconLock } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { ErrorNote, PageHeader, PageLoader, Section } from '../../components/ui.jsx';

const SIZES = ['1-10', '11-50', '51-200', '201-500', '500+'];

export default function EmployerProfile() {
  const location = useLocation();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const form = useForm({
    initialValues: { companyName: '', website: '', industry: '', companySize: null, about: '', country: '', foundedYear: '', contactPhone: '' },
    validate: {
      companyName: (v) => (v.trim().length >= 2 ? null : 'Enter your company name'),
      about: (v) => (v.trim().length >= 20 ? null : 'Describe your company in at least 20 characters'),
      website: (v) => (!v || /^https?:\/\/\S{3,300}$/.test(v) ? null : 'Enter a full link starting with http:// or https://'),
      contactPhone: (v) => (!v || /^[0-9+()\-\s]{6,20}$/.test(v) ? null : 'Use digits, spaces, + ( ) or -'),
    },
  });

  const fill = (p) => {
    setProfile(p);
    form.setValues({
      companyName: p.companyName || '', website: p.website || '', industry: p.industry || '', companySize: p.companySize || null,
      about: p.about || '', country: p.country || '', foundedYear: p.foundedYear ?? '', contactPhone: p.contactPhone || '',
    });
    form.resetDirty();
  };
  useEffect(() => { api.get('/employer/profile').then((r) => fill(r.data)).catch((e) => setError(errorMessage(e))); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const save = async (v) => {
    setBusy(true);
    try {
      const { data } = await api.put('/employer/profile', { ...v, foundedYear: v.foundedYear === '' ? null : v.foundedYear, companySize: v.companySize || null });
      fill(data);
      notifications.show({ color: 'teal', title: 'Company profile saved', message: 'You can now post jobs.' });
    } catch (e) { notifications.show({ color: 'red', title: 'Could not save', message: errorMessage(e) }); }
    finally { setBusy(false); }
  };

  if (error) return <ErrorNote message={error} />;
  if (!profile) return <PageLoader />;

  return (
    <form onSubmit={form.onSubmit(save)} noValidate className="max-w-3xl space-y-6">
      <PageHeader title="Company profile" subtitle="Candidates see this on every job you post."
        actions={<Button type="submit" loading={busy}>Save profile</Button>} />
      {location.state?.welcome && <Alert color="teal" variant="light" title="Welcome aboard">Add your company name and a short description, then post your first job.</Alert>}
      <Group gap="xs">
        {profile.verified ? <Badge color="teal" variant="light">Verified company</Badge> : <Badge color="gray" variant="light">Not verified yet</Badge>}
        <Text size="sm" c="dimmed">An administrator verifies companies. A verified badge shows next to your jobs.</Text>
      </Group>
      <Section title="About the company">
        <TextInput label="Company name" {...form.getInputProps('companyName')} />
        <Textarea label="What does the company do?" autosize minRows={3} maxRows={10} maxLength={3000} {...form.getInputProps('about')} />
        <TextInput label="Website" placeholder="https://" {...form.getInputProps('website')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextInput label="Industry" {...form.getInputProps('industry')} />
          <Select label="Company size" data={SIZES} clearable placeholder="Select" {...form.getInputProps('companySize')} />
          <TextInput label="Country" {...form.getInputProps('country')} />
          <NumberInput label="Founded in" min={1800} max={2100} allowDecimal={false} hideControls {...form.getInputProps('foundedYear')} />
        </div>
      </Section>
      <Section title="Private contact" aside={<Group gap={6} c="dimmed"><IconLock size={16} /><Text size="sm">Encrypted, never shown publicly</Text></Group>}>
        <TextInput label="Contact phone" {...form.getInputProps('contactPhone')} />
      </Section>
      <Text size="sm" c="dimmed">Ready? <Link to="/employer/post-job" className="font-semibold text-signal">Post a job</Link></Text>
    </form>
  );
}
