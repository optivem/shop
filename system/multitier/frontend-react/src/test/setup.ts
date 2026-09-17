// Global test setup — registers jest-dom matchers. Testing Library unmounts rendered trees after
// each test by itself, because vite.config.ts enables vitest globals (afterEach is global).
import '@testing-library/jest-dom/vitest';
