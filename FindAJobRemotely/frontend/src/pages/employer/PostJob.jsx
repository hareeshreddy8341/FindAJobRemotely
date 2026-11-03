import { Button, NumberInput, Select, Stack, TagsInput, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { ErrorNote, PageHeader, PageLoader } from '../../components/ui.jsx';
import { CURRENCIES, JOB_TYPES, LEVELS } from '../../lib/format.js';

/** Used for both "Post a job" and "Edit job" (when the URL has an id). */
export default function PostJob() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(Boolean(id));
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const form = useForm({
    initialValues: {
      title: '', description: '', type: 'FULL_TIME', experienceLevel: 'JUNIOR', skills: [],
      salaryMin: '', salaryMax: '', currency: null, remoteRegion: 'Worldwide', openings: 1,
    },
    validate: {
      title: (v) => (v.trim().length >= 3 ? null : 'Enter a job title'),
      description: (v) => (v.trim().length >= 20 ? null : 'Describe the role in at least 20 characters'),
      skills: (v) => (v.length ? null : 'Add at least one required skill'),
      remoteRegion: (v) => (v.trim() ? null : 'Say where candidates can be based'),
      currency: (v, vals) => ((vals.salaryMin !== '' || vals.salaryMax !== '') && !v ? 'Select a currency' : null),
      salaryMax: (v, vals) => (v !== '' && vals.salaryMin !== '' && Number(v) < Number(vals.salaryMin) ? 'Maximum must be at least the minimum' : null),
      openings: (v) => (v >= 1 ? null : 'At least one opening'),
    },
  });

  useEffect(() => {
    if (!id) return;
    api.get(`/jobs/${id}`).then(({ data: j }) => {
      form.setValues({
        title: j.title, description: j.description, type: j.type, experienceLevel: j.experienceLevel, skills: j.skills,
        salaryMin: j.salaryMin ?? '', salaryMax: j.salaryMax ?? '', currency: j.currency || null,
        remoteRegion: j.remoteRegion, openings: j.openings,
      });
      setLoading(false);
    }).catch((e) => { setError(errorMessage(e)); setLoading(false); });
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const submit = async (v) => {
    setBusy(true);
    try {
      const body = { ...v, salaryMin: v.salaryMin === '' ? null : v.salaryMin, salaryMax: v.salaryMax === '' ? null : v.salaryMax, currency: v.currency || null };
      if (id) await api.put(`/employer/jobs/${id}`, body); else await api.post('/employer/jobs', body);
      notifications.show({ color: 'teal', title: id ? 'Job updated' : 'Job published', message: id ? 'Your changes are live.' : 'Candidates can now find and apply to it.' });
      navigate('/employer/jobs');
    } catch (e) {
      notifications.show({ color: 'red', title: 'Could not save the job', message: errorMessage(e) });
    } finally { setBusy(false); }
  };

  if (loading) return <PageLoader />;
  return (
    <div className="max-w-3xl">
      <PageHeader title={id ? 'Edit job' : 'Post a job'} subtitle="Be specific about skills. We use them to match candidates and rank applicants." />
      <ErrorNote message={error} />
      <form onSubmit={form.onSubmit(submit)} noValidate className="rounded-xl border border-slate-200 bg-white p-5">
        <Stack>
          <TextInput label="Job title" placeholder="Frontend developer" maxLength={120} {...form.getInputProps('title')} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Select label="Job type" data={JOB_TYPES} {...form.getInputProps('type')} />
            <Select label="Experience level" data={LEVELS} {...form.getInputProps('experienceLevel')} />
          </div>
          <Textarea label="Description" description="What will this person do, and what does a good week look like?" autosize minRows={6} maxRows={18} maxLength={8000} {...form.getInputProps('description')} />
          <TagsInput label="Required skills" description="Press Enter after each skill" maxTags={30} {...form.getInputProps('skills')} />
          <div className="grid gap-4 sm:grid-cols-3">
            <NumberInput label="Monthly pay from" min={0} allowDecimal={false} thousandSeparator="," {...form.getInputProps('salaryMin')} />
            <NumberInput label="Monthly pay up to" min={0} allowDecimal={false} thousandSeparator="," {...form.getInputProps('salaryMax')} />
            <Select label="Currency" data={CURRENCIES} clearable placeholder="Select" {...form.getInputProps('currency')} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextInput label="Where can candidates be based?" maxLength={80} {...form.getInputProps('remoteRegion')} />
            <NumberInput label="Number of openings" min={1} max={500} allowDecimal={false} {...form.getInputProps('openings')} />
          </div>
          <div className="flex gap-2">
            <Button type="submit" loading={busy}>{id ? 'Save changes' : 'Publish job'}</Button>
            <Button variant="default" onClick={() => navigate('/employer/jobs')}>Cancel</Button>
          </div>
        </Stack>
      </form>
    </div>
  );
}
