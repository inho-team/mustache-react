import { createElement, type ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { useServerContext } from '@mustache-react/client';
import { createRenderer } from './render.js';
import { handleRender, portFromArgs, startServer } from './http.js';

const Greeting = ({ name }: { name: string }) => {
  const { user } = useServerContext();
  return createElement('p', null, `hello ${name}, ${user?.name ?? 'anonymous'}`);
};
const Wrapper = ({ children }: { children?: ReactNode }) => createElement('main', null, children);

const render = createRenderer({
  components: { '/pages/Greeting.tsx': { default: Greeting }, '/islands/NotAComponent.tsx': {} },
  wrapper: Wrapper,
});

describe('createRenderer', () => {
  it('renders the component inside the wrapper with the server context', () => {
    expect(render({ component: 'pages/Greeting', props: { name: 'kim' }, context: { user: { name: 'lee', authorities: [] } } }))
      .toBe('<main><p>hello kim, lee</p></main>');
  });

  it('names the known components when one is missing', () => {
    expect(() => render({ component: 'pages/Missing', props: {} })).toThrow('Known components: pages/Greeting');
  });
});

describe('render endpoint', () => {
  it('answers html, or an error with status 500', async () => {
    expect(await handleRender(() => render, JSON.stringify({ component: 'pages/Greeting', props: { name: 'a' } }))).toEqual({
      status: 200,
      json: { html: '<main><p>hello a, anonymous</p></main>' },
    });
    expect((await handleRender(() => render, '{"component":"pages/Missing","props":{}}')).status).toBe(500);
    expect(await handleRender(() => render, 'not json')).toEqual({ status: 400, json: { error: 'Invalid JSON' } });
  });

  it('serves render requests over HTTP on 127.0.0.1', async () => {
    const server = await startServer(render, 0);
    try {
      const { port } = server.address() as { port: number };
      const response = await fetch(`http://127.0.0.1:${port}/__mustache-react/render`, {
        method: 'POST',
        body: JSON.stringify({ component: 'pages/Greeting', props: { name: '김' } }),
      });
      expect(await response.json()).toEqual({ html: '<main><p>hello 김, anonymous</p></main>' });
      expect((await fetch(`http://127.0.0.1:${port}/__mustache-react/health`)).status).toBe(200);
      expect((await fetch(`http://127.0.0.1:${port}/other`)).status).toBe(404);
    } finally {
      server.close();
    }
  });

  it('reads the port argument', () => {
    expect(portFromArgs(['--port', '5174'])).toBe(5174);
    expect(() => portFromArgs([])).toThrow('Usage');
  });
});
