import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The browser talks only to our backend (CLAUDE.md §6); /api, including the SSE stream, is proxied to it.
export default defineConfig({
  plugins: [react()],
  build: { chunkSizeWarningLimit: 1500 }, // one demo app, no code splitting needed
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
