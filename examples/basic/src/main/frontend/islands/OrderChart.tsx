import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { Order } from '../types';

interface Props {
  orders: Order[];
  currency: string;
}

/** Any React library works in an island; this one uses recharts. */
export default function OrderChart({ orders, currency }: Props) {
  const data = orders
    .filter((order) => order.status !== 'CANCELLED')
    .map((order) => ({ name: String(order.id), amount: order.amount }));

  return (
    <figure className="chart" data-testid="order-chart">
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="name" />
          <YAxis />
          <Tooltip formatter={(value) => `${Number(value).toLocaleString('ko-KR')}${currency}`} />
          <Bar dataKey="amount" fill="#2f6feb" isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
      <figcaption>주문 금액 (취소 제외)</figcaption>
    </figure>
  );
}
