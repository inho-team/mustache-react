import type { ReactNode } from 'react';
import './app.css';

/** Wraps every page and island. Put theme, query client or i18n providers here. */
export default function App({ children }: { children?: ReactNode }) {
  return <div className="app">{children}</div>;
}
