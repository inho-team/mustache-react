// In M4 the Gradle/Maven plugin supplies this configuration; the example spells it out until then.
import react from '@vitejs/plugin-react';
import { mustacheReact } from '@mustache-react/vite-plugin';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react(), mustacheReact()],
});
