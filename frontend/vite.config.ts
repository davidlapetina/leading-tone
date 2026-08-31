import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// The backend is a separate process; proxying keeps the browser on one origin so that
// Web MIDI's secure-context rules and CORS both stay out of the way in development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    // e2e/ belongs to Playwright; Vitest must not try to run it.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
})
