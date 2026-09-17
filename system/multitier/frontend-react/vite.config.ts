import { defineConfig } from 'vite';
import { configDefaults } from 'vitest/config';
import react from '@vitejs/plugin-react';

// An empty BACKEND_API_URL counts as unset, so this is deliberately not `??`.
const backendApiUrl = process.env.BACKEND_API_URL;

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: false,
    // Pact spins up a real local mock HTTP server per test; give it room.
    testTimeout: 30000,
    hookTimeout: 30000,
    // Consumer test files share one pact contract file (frontend-react-backend.json);
    // run files sequentially so concurrent writers don't race on the merge.
    fileParallelism: false,
    // OPT-IN LAYER: the in-process component + Pact contract suites are kept OFF
    // the default `npm test` so the default build is unchanged for students who
    // never opted in. They run via `npm run test:component` / `test:pact`
    // (vitest.opt-in.config.ts). Default `npm test` runs only the fast,
    // no-network unit tests at the top level of src/test (e.g. harness.test.tsx).
    exclude: [...configDefaults.exclude, 'src/test/legacy/**', 'src/test/latest/**'],
  },
  publicDir: 'public',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: backendApiUrl !== undefined && backendApiUrl !== '' ? backendApiUrl : 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
});
