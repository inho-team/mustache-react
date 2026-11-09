import { useMessages } from '@mustache-react/client';
import { useState } from 'react';
import type { IslandProps } from '../generated/types';

/** Props: @ReactIsland("StatusBadge") in Java (StatusBadgeProps). */
export default function StatusBadge({ status, orderId }: IslandProps<'StatusBadge'>) {
  const { t } = useMessages();
  const [open, setOpen] = useState(false);
  return (
    <button type="button" className={`badge badge-${status.toLowerCase()}`} onClick={() => setOpen(!open)}>
      {t(`orders.status.${status}`)}
      {open && <span className="badge-detail"> · {t('orders.status.detail', String(orderId))}</span>}
    </button>
  );
}
