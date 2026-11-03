import axios from 'axios';
import { sessionStarted, signedOut } from '../store/authSlice.js';

let store;
export const injectStore = (s) => { store = s; };

const api = axios.create({ baseURL: '/api', withCredentials: true, timeout: 20000 });

api.interceptors.request.use((config) => {
  const token = store?.getState().auth.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// One refresh at a time, shared by every request that fails at the same moment.
let inFlight = null;
export function refreshSession() {
  if (!inFlight) {
    inFlight = axios
      .post('/api/auth/refresh', null, { withCredentials: true, timeout: 20000 })
      .then((r) => r.data)
      .finally(() => { inFlight = null; });
  }
  return inFlight;
}

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const { config, response } = error;
    const isAuthCall = config?.url?.startsWith('/auth/') && !config.url.startsWith('/auth/me');
    if (response?.status === 401 && config && !config._retried && !isAuthCall && store?.getState().auth.accessToken) {
      config._retried = true;
      try {
        const data = await refreshSession();
        store.dispatch(sessionStarted(data));
        config.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(config);
      } catch {
        store.dispatch(signedOut());
      }
    }
    return Promise.reject(error);
  },
);

export function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (err?.response?.data) {
    const d = err.response.data;
    if (d.fieldErrors && Object.keys(d.fieldErrors).length) {
      const [k, v] = Object.entries(d.fieldErrors)[0];
      return `${k}: ${v}`;
    }
    if (d.message) return d.message;
  }
  if (err?.response?.status === 429) return 'Too many requests. Please wait a minute and try again.';
  if (err?.code === 'ECONNABORTED') return 'The server took too long to answer. Please try again.';
  if (!err?.response) return 'Could not reach the server. Check your connection and try again.';
  return fallback;
}

export default api;
