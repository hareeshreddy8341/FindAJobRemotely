import api from '../api/client.js';

/** Fetches a protected file with the Authorization header (a plain link could not) and saves it. */
export async function downloadPdf(url, fallbackName = 'resume.pdf') {
  const res = await api.get(url, { responseType: 'blob' });
  const blobUrl = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
  const a = document.createElement('a');
  a.href = blobUrl;
  a.download = fallbackName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(blobUrl), 10000);
}
