import { Alert } from '@mantine/core';

export function AuthShell({ title, subtitle, children, dark = false }) {
  return (
    <div className="mx-auto w-full max-w-md py-6">
      <div className={`rounded-2xl border p-6 md:p-8 ${dark ? 'border-ink bg-ink text-white' : 'border-slate-200 bg-white'}`}>
        <h1 className="font-display text-2xl font-bold">{title}</h1>
        {subtitle && <p className={`mt-1 text-sm ${dark ? 'text-slate-300' : 'text-slate-600'}`}>{subtitle}</p>}
        <div className="mt-5">{children}</div>
      </div>
    </div>
  );
}

export const FormError = ({ message }) =>
  message ? <Alert color="red" variant="light" role="alert" mb="md">{message}</Alert> : null;
