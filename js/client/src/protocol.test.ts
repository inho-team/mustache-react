import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { Protocol } from './protocol.js';

const javaProtocol = fileURLToPath(
  new URL(
    '../../../java/mustache-react-core/src/main/java/run/iknow/mustachereact/core/Protocol.java',
    import.meta.url,
  ),
);

describe('Protocol', () => {
  it('matches the Java constants', () => {
    const source = readFileSync(javaProtocol, 'utf8');
    const javaConstants = Object.fromEntries(
      [...source.matchAll(/public static final String (\w+) = "([^"]*)";/g)].map((m) => [m[1], m[2]]),
    );
    expect(javaConstants).toEqual({ ...Protocol });
  });
});
