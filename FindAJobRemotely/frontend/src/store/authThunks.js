import api, { refreshSession } from '../api/client.js';
import { sessionStarted, signedOut } from './authSlice.js';

export const bootstrapSession = () => async (dispatch) => {
  try {
    dispatch(sessionStarted(await refreshSession()));
  } catch {
    dispatch(signedOut());
  }
};

export const login = (values, adminPortal = false) => async (dispatch) => {
  const { data } = await api.post(adminPortal ? '/auth/admin/login' : '/auth/login', values);
  dispatch(sessionStarted(data));
  return data.user;
};

export const register = (values) => async (dispatch) => {
  const { data } = await api.post('/auth/register', values);
  dispatch(sessionStarted(data));
  return data.user;
};

export const logout = () => async (dispatch) => {
  try { await api.post('/auth/logout'); } catch { /* the cookie expires on its own */ }
  dispatch(signedOut());
};
