import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The officer routes of the Spring Boot backend send no CORS headers, so in development the browser talks to
// this dev server and /backend/* is proxied to the backend (default http://localhost:8080).
const target = process.env.BACKEND_URL || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/backend': { target, changeOrigin: true, rewrite: (p) => p.replace(/^\/backend/, '') },
    },
  },
});
