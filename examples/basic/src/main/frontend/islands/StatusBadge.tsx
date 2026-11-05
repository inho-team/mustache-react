import { useState } from 'react';
import type { OrderStatus } from '../types';

const LABELS: Record<OrderStatus, string> = { PAID: '결제 완료', SHIPPED: '배송 중', CANCELLED: '취소' };

interface Props {
  status: OrderStatus;
  orderId: number;
}

export default function StatusBadge({ status, orderId }: Props) {
  const [open, setOpen] = useState(false);
  return (
    <button type="button" className={`badge badge-${status.toLowerCase()}`} onClick={() => setOpen(!open)}>
      {LABELS[status]}
      {open && <span className="badge-detail"> · 주문 #{orderId}</span>}
    </button>
  );
}
