import type { ReactNode } from 'react';
import './app.css';

/**
 * Wraps every page and island. Put theme, query client or i18n providers here.
 * Islands can sit inside table cells, so avoid adding layout elements.
 */
export default function App({ children }: { children?: ReactNode }) {
  return <>{children}</>;
}
