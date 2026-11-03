import JobSearch from '../components/JobSearch.jsx';
import { PageHeader } from '../components/ui.jsx';

export default function Jobs() {
  return (
    <>
      <PageHeader title="Remote jobs and internships" subtitle="Every role is remote and posted by a startup. Browse freely, then create a profile to apply." />
      <JobSearch />
    </>
  );
}
