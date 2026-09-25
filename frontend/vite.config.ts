import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  define: mode === 'production' ? {
    'console.log': '(() => {})',
    'console.debug': '(() => {})',
    'console.info': '(() => {})',
  } : {},
}))
