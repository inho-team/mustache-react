import { createServer, type Server } from 'node:http';
import type { Renderer, RenderRequest } from './render.js';

/** Path of the render endpoint, on the SSR server and on the Vite dev server. */
export const RENDER_PATH = '/__mustache-react/render';

const MAX_BODY = 16 * 1024 * 1024;

/**
 * Handles `POST /__mustache-react/render` with `{component, props, context}`; answers `{html}`, or `{error}` with
 * status 500. Shared by the production SSR server and the dev server middleware, which loads the renderer anew for
 * each request so that edits apply.
 */
export async function handleRender(
  getRenderer: () => Renderer | Promise<Renderer>,
  body: string,
): Promise<{ status: number; json: unknown }> {
  let request: RenderRequest;
  try {
    request = JSON.parse(body) as RenderRequest;
  } catch {
    return { status: 400, json: { error: 'Invalid JSON' } };
  }
  try {
    const renderer = await getRenderer();
    return { status: 200, json: { html: renderer(request) } };
  } catch (error) {
    return { status: 500, json: { error: error instanceof Error ? (error.stack ?? error.message) : String(error) } };
  }
}

/** Reads a request body. */
export function readBody(request: NodeJS.ReadableStream & { destroy?: () => void }): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    let size = 0;
    request.on('data', (chunk: Buffer) => {
      size += chunk.length;
      if (size > MAX_BODY) {
        reject(new Error('Request too large'));
        request.destroy?.();
        return;
      }
      chunks.push(chunk);
    });
    request.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    request.on('error', reject);
  });
}

/**
 * Starts the SSR HTTP server Spring talks to. Listens on 127.0.0.1 only: it is a sidecar of the application,
 * not a public service.
 */
export function startServer(render: Renderer, port: number, host = '127.0.0.1'): Promise<Server> {
  const server = createServer((req, res) => {
    if (req.method === 'GET' && req.url === '/__mustache-react/health') {
      res.writeHead(200, { 'Content-Type': 'text/plain' }).end('ok');
      return;
    }
    if (req.method !== 'POST' || req.url !== RENDER_PATH) {
      res.writeHead(404).end();
      return;
    }
    readBody(req)
      .then((body) => handleRender(() => render, body))
      .then(({ status, json }) => {
        res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' }).end(JSON.stringify(json));
      })
      .catch((error: unknown) => {
        res.writeHead(500, { 'Content-Type': 'application/json; charset=utf-8' }).end(JSON.stringify({ error: String(error) }));
      });
  });
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, host, () => resolve(server));
  });
}

/** `--port <n>` from the command line. */
export function portFromArgs(argv: string[]): number {
  const index = argv.indexOf('--port');
  const port = index >= 0 ? Number(argv[index + 1]) : NaN;
  if (!Number.isInteger(port) || port <= 0) {
    throw new Error('Usage: node server.mjs --port <port>');
  }
  return port;
}
