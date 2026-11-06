import { useMessages } from '@mustache-react/client';
import { useState } from 'react';
import type { OrderStatus } from '../generated/types';

interface Props {
  status: OrderStatus;
  orderId: number;
}

export default function StatusBadge({ status, orderId }: Props) {
  const { t } = useMessages();
  const [open, setOpen] = useState(false);
  return (
    <button type="button" className={`badge badge-${status.toLowerCase()}`} onClick={() => setOpen(!open)}>
      {t(`orders.status.${status}`)}
      {open && <span className="badge-detail"> · {t('orders.status.detail', String(orderId))}</span>}
    </button>
  );
}
