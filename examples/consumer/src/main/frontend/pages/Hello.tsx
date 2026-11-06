import { useState } from 'react';
import type { PageProps } from '../generated/types';

export default function Hello({ message, frameworks }: PageProps<'Hello'>) {
  const [count, setCount] = useState(0);
  return (
    <main>
      <h1>{message}</h1>
      <ul>
        {frameworks.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
      <button type="button" onClick={() => setCount(count + 1)}>
        clicked {count} times
      </button>
    </main>
  );
}
