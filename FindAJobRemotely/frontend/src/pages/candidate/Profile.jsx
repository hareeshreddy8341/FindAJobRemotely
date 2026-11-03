import { Alert, Button, FileInput, Group, MultiSelect, NumberInput, Select, Stack, Switch, TagsInput, Text, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconLock, IconWorld } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useLocation } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { ErrorNote, PageHeader, PageLoader, Section } from '../../components/ui.jsx';
import { CURRENCIES, JOB_TYPES, LEVELS } from '../../lib/format.js';
import { downloadPdf } from '../../lib/download.js';
import { userUpdated } from '../../store/authSlice.js';

const urlOk = (v) => (!v || /^https?:\/\/\S{3,300}$/.test(v) ? null : 'Enter a full link starting with http:// or https://');

export default function Profile() {
  const dispatch = useDispatch();
  const location = useLocation();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  const form = useForm({
    initialValues: {
      fullName: '', headline: '', about: '', skills: [], experienceLevel: null, yearsOfExperience: '',
      preferredJobTypes: [], preferredRoles: [], education: '', country: '', timezone: '',
      githubUrl: '', linkedinUrl: '', portfolioUrl: '', publicProfile: true,
      phone: '', expectedMonthlySalary: '', currency: null,
    },
    validate: {
      fullName: (v) => (v.trim().length >= 2 ? null : 'Enter your full name'),
      headline: (v) => (v.trim().length >= 3 ? null : 'Add a short headline, e.g. “React developer”'),
      skills: (v) => (v.length ? null : 'Add at least one skill'),
      githubUrl: urlOk, linkedinUrl: urlOk, portfolioUrl: urlOk,
      phone: (v) => (!v || /^[0-9+()\-\s]{6,20}$/.test(v) ? null : 'Use digits, spaces, + ( ) or -'),
      currency: (v, vals) => (vals.expectedMonthlySalary !== '' && !v ? 'Select a currency' : null),
    },
  });

  const fill = (p) => {
    setProfile(p);
    form.setValues({
      fullName: p.fullName || '', headline: p.headline || '', about: p.about || '', skills: p.skills || [],
      experienceLevel: p.experienceLevel || null, yearsOfExperience: p.yearsOfExperience ?? '',
      preferredJobTypes: p.preferredJobTypes || [], preferredRoles: p.preferredRoles || [],
      education: p.education || '', country: p.country || '', timezone: p.timezone || '',
      githubUrl: p.githubUrl || '', linkedinUrl: p.linkedinUrl || '', portfolioUrl: p.portfolioUrl || '',
      publicProfile: p.publicProfile, phone: p.phone || '', expectedMonthlySalary: p.expectedMonthlySalary ?? '', currency: p.currency || null,
    });
    form.resetDirty();
  };

  useEffect(() => {
    api.get('/candidate/profile').then((r) => fill(r.data)).catch((e) => setError(errorMessage(e)));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const save = async (v) => {
    setBusy(true);
    try {
      const body = {
        ...v,
        yearsOfExperience: v.yearsOfExperience === '' ? null : v.yearsOfExperience,
        expectedMonthlySalary: v.expectedMonthlySalary === '' ? null : v.expectedMonthlySalary,
        experienceLevel: v.experienceLevel || null,
        currency: v.currency || null,
      };
      const { data } = await api.put('/candidate/profile', body);
      fill(data);
      dispatch(userUpdated({ fullName: data.fullName }));
      notifications.show({ color: 'teal', title: 'Profile saved', message: 'Your matches have been updated.' });
    } catch (e) {
      notifications.show({ color: 'red', title: 'Could not save profile', message: errorMessage(e) });
    } finally { setBusy(false); }
  };

  const upload = async () => {
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) { notifications.show({ color: 'red', title: 'File too large', message: 'The resume must be 2 MB or smaller.' }); return; }
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      await api.post('/candidate/resume', fd);
      setFile(null);
      const { data } = await api.get('/candidate/profile');
      setProfile(data);
      notifications.show({ color: 'teal', title: 'Resume uploaded', message: 'It is encrypted and shared only with companies you apply to.' });
    } catch (e) {
      notifications.show({ color: 'red', title: 'Upload failed', message: errorMessage(e) });
    } finally { setUploading(false); }
  };

  const removeResume = async () => {
    try {
      await api.delete('/candidate/resume');
      setProfile((p) => ({ ...p, hasResume: false, resumeFileName: null }));
    } catch (e) { notifications.show({ color: 'red', title: 'Could not remove resume', message: errorMessage(e) }); }
  };

  if (error) return <ErrorNote message={error} />;
  if (!profile) return <PageLoader label="Loading your profile" />;

  return (
    <form onSubmit={form.onSubmit(save)} noValidate className="max-w-3xl space-y-6">
      <PageHeader title="My profile" subtitle="Complete profiles get better matches and are easier for startups to find."
        actions={<Button type="submit" loading={busy}>Save profile</Button>} />
      {location.state?.welcome && (
        <Alert color="teal" variant="light" title="Welcome aboard">Add your headline and skills first. Those two fields unlock job matching and applying.</Alert>
      )}

      <Section title="Public profile" aside={<Group gap={6} c="dimmed"><IconWorld size={16} /><Text size="sm">Visible to signed-in employers</Text></Group>}>
        <TextInput label="Full name" {...form.getInputProps('fullName')} />
        <TextInput label="Headline" placeholder="React developer open to internships" maxLength={120} {...form.getInputProps('headline')} />
        <Textarea label="About you" autosize minRows={3} maxRows={10} maxLength={3000} {...form.getInputProps('about')} />
        <TagsInput label="Skills" description="Press Enter after each skill. Up to 30." maxTags={30} {...form.getInputProps('skills')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Select label="Experience level" data={LEVELS} clearable placeholder="Select" {...form.getInputProps('experienceLevel')} />
          <NumberInput label="Years of experience" min={0} max={60} allowDecimal={false} {...form.getInputProps('yearsOfExperience')} />
        </div>
        <MultiSelect label="Kinds of work you want" data={JOB_TYPES} {...form.getInputProps('preferredJobTypes')} />
        <TagsInput label="Roles you are looking for" description="e.g. Data analyst, Frontend developer" maxTags={10} {...form.getInputProps('preferredRoles')} />
        <TextInput label="Education" {...form.getInputProps('education')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextInput label="Country" {...form.getInputProps('country')} />
          <TextInput label="Time zone" placeholder="e.g. UTC+5:30" {...form.getInputProps('timezone')} />
        </div>
        <TextInput label="GitHub" placeholder="https://github.com/you" {...form.getInputProps('githubUrl')} />
        <TextInput label="LinkedIn" placeholder="https://linkedin.com/in/you" {...form.getInputProps('linkedinUrl')} />
        <TextInput label="Portfolio" placeholder="https://" {...form.getInputProps('portfolioUrl')} />
        <Switch label="Let employers find my profile and posts" description="Turn this off to hide your profile from talent search. You can still apply to jobs."
          {...form.getInputProps('publicProfile', { type: 'checkbox' })} />
      </Section>

      <Section title="Private details" aside={<Group gap={6} c="dimmed"><IconLock size={16} /><Text size="sm">Encrypted. Shared only when you apply</Text></Group>}>
        <TextInput label="Phone number" autoComplete="tel" {...form.getInputProps('phone')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <NumberInput label="Expected monthly pay" min={0} allowDecimal={false} thousandSeparator="," {...form.getInputProps('expectedMonthlySalary')} />
          <Select label="Currency" data={CURRENCIES} clearable placeholder="Select" {...form.getInputProps('currency')} />
        </div>
        <Text size="sm" c="dimmed">Your expected pay is used only to rank jobs for you. Employers never see it.</Text>
      </Section>

      <Section title="Resume">
        {profile.hasResume ? (
          <Group justify="space-between">
            <div>
              <Text fw={600}>{profile.resumeFileName}</Text>
              <Text size="sm" c="dimmed">{Math.round((profile.resumeSizeBytes || 0) / 1024)} KB, encrypted</Text>
            </div>
            <Group gap="xs">
              <Button type="button" variant="default" size="compact-md" onClick={() => downloadPdf('/candidate/resume', profile.resumeFileName).catch((e) => notifications.show({ color: 'red', title: 'Download failed', message: errorMessage(e) }))}>Download</Button>
              <Button type="button" variant="subtle" color="red" size="compact-md" onClick={removeResume}>Remove</Button>
            </Group>
          </Group>
        ) : <Text size="sm" c="dimmed">No resume uploaded yet.</Text>}
        <Group align="end">
          <FileInput label={profile.hasResume ? 'Replace resume' : 'Upload resume'} description="PDF only, up to 2 MB" accept="application/pdf" value={file} onChange={setFile} clearable style={{ flex: 1, minWidth: 220 }} />
          <Button type="button" onClick={upload} disabled={!file} loading={uploading}>Upload</Button>
        </Group>
      </Section>
    </form>
  );
}
