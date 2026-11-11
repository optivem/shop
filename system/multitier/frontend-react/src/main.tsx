import React from 'react';
import ReactDOM from 'react-dom/client';
import 'bootstrap/dist/css/bootstrap.min.css';
import '@fontsource-variable/figtree';
import './styles/market.css';
import './styles/pages-shop.css';
import './styles/pages-orders.css';
import './styles/pages-account.css';
import { App } from './App';
import { AuthGate } from './auth/AuthGate';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found');
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <AuthGate>
      <App />
    </AuthGate>
  </React.StrictMode>
);
