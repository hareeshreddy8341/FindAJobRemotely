import { Button, Group, Select, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { JOB_TYPES, LEVELS } from '../lib/format.js';

/** Controlled by the Redux filters. The text box waits for a pause in typing before searching. */
export default function JobFilters({ filters, onChange, onReset }) {
  const [q, setQ] = useState(filters.q);
  const [skill, setSkill] = useState(filters.skill);

  useEffect(() => { setQ(filters.q); setSkill(filters.skill); }, [filters.q, filters.skill]);
  useEffect(() => {
    if (q === filters.q && skill === filters.skill) return undefined;
    const t = setTimeout(() => onChange({ q, skill, page: 0 }), 350);
    return () => clearTimeout(t);
  }, [q, skill]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <form role="search" onSubmit={(e) => { e.preventDefault(); onChange({ q, skill, page: 0 }); }}
      className="mb-5 grid gap-3 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-[2fr_1fr_1fr_1fr_auto]">
      <TextInput aria-label="Search by title or company" placeholder="Job title, company or skill"
        leftSection={<IconSearch size={16} />} value={q} onChange={(e) => setQ(e.currentTarget.value)} maxLength={80} />
      <TextInput aria-label="Skill" placeholder="Exact skill, e.g. React" value={skill}
        onChange={(e) => setSkill(e.currentTarget.value)} maxLength={40} />
      <Select aria-label="Job type" placeholder="Any job type" clearable value={filters.type}
        data={JOB_TYPES} onChange={(v) => onChange({ type: v, page: 0 })} />
      <Select aria-label="Experience level" placeholder="Any level" clearable value={filters.level}
        data={LEVELS} onChange={(v) => onChange({ level: v, page: 0 })} />
      <Group><Button variant="default" onClick={onReset}>Clear</Button></Group>
    </form>
  );
}
