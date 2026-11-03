import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import './index.css';

import { MantineProvider } from '@mantine/core';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { injectStore } from './api/client.js';
import { store } from './store/index.js';
import { theme } from './theme.js';

injectStore(store);

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <MantineProvider theme={theme} forceColorScheme="light">
        <ModalsProvider>
          <Notifications position="top-right" />
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ModalsProvider>
      </MantineProvider>
    </Provider>
  </React.StrictMode>,
);
