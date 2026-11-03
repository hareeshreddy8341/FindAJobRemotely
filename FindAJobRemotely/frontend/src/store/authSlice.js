import { createSlice } from '@reduxjs/toolkit';

// The access token lives only in memory (never localStorage), so a script injected into the page
// cannot read it from storage. A page reload restores the session from the HttpOnly refresh cookie.
const slice = createSlice({
  name: 'auth',
  initialState: { user: null, accessToken: null, status: 'loading' },
  reducers: {
    sessionStarted(state, { payload }) {
      state.user = payload.user;
      state.accessToken = payload.accessToken;
      state.status = 'ready';
    },
    signedOut(state) {
      state.user = null;
      state.accessToken = null;
      state.status = 'ready';
    },
    userUpdated(state, { payload }) {
      if (state.user) state.user = { ...state.user, ...payload };
    },
  },
});

export const { sessionStarted, signedOut, userUpdated } = slice.actions;
export default slice.reducer;
