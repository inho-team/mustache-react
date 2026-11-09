import { useMessages } from '@mustache-react/client';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { IslandProps } from '../generated/types';

/** Any React library works in an island; this one uses recharts. Props: @ReactIsland("OrderChart") in Java. */
export default function OrderChart({ orders, currency }: IslandProps<'OrderChart'>) {
  const { t } = useMessages();
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
      <figcaption>{t('orders.chart.caption')}</figcaption>
    </figure>
  );
}
