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

function render(root: HTMLElement): void {
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <AuthGate>
        <App />
      </AuthGate>
    </React.StrictMode>
  );
}

// Browser tracing is opt-in via /config.js. When it is off the tracing chunk is never loaded and the app renders
// synchronously as before; when on, it is started before the first render so the initial API calls are traced.
const otlpEndpoint = globalThis.window.__APP_CONFIG__?.otelExporterOtlpEndpoint;
if (otlpEndpoint !== undefined && otlpEndpoint !== '') {
  import('./telemetry/tracing')
    .then(({ startTracing }) => startTracing(otlpEndpoint))
    .catch((error: unknown) => console.warn('Browser tracing failed to start', error))
    .finally(() => render(rootElement));
} else {
  render(rootElement);
}
