import { defineConfig } from 'vitest/config';

export default defineConfig({
  resolve: {
    alias: {
      react: new URL('./node_modules/react', import.meta.url).pathname,
      'react-test-renderer': new URL(
        './node_modules/react-test-renderer',
        import.meta.url
      ).pathname,
    },
    dedupe: ['react', 'react-test-renderer'],
  },
  test: {
    environment: 'node',
    globals: false,
    include: ['core/**/*.test.ts', 'core/**/*.test.tsx'],
  },
});
