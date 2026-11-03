import { createSlice } from '@reduxjs/toolkit';

// Remembers the job-search filters while the person moves between pages, so coming back from a
// job's detail page does not reset what they were looking at.
const initial = { q: '', type: null, level: null, skill: '', page: 0 };

const slice = createSlice({
  name: 'jobs',
  initialState: { filters: initial },
  reducers: {
    setFilters(state, { payload }) {
      state.filters = { ...state.filters, ...payload };
    },
    resetFilters(state) {
      state.filters = initial;
    },
  },
});

export const { setFilters, resetFilters } = slice.actions;
export default slice.reducer;
