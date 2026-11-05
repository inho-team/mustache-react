import { describe, expect, it } from 'vitest';
import { mustacheReact } from './index.js';

describe('mustacheReact', () => {
  it('uses src/main/frontend as the Vite root and enables the manifest', () => {
    const plugin = mustacheReact();
    const config = (plugin.config as () => Record<string, unknown>)();
    expect(plugin.name).toBe('mustache-react');
    expect(config).toEqual({ root: 'src/main/frontend', build: { manifest: true } });
  });
});
