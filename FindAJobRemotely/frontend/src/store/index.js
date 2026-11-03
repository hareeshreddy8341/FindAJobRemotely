import { configureStore } from '@reduxjs/toolkit';
import auth from './authSlice.js';
import jobs from './jobsSlice.js';

export const store = configureStore({ reducer: { auth, jobs } });
