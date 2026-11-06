import { useMessages } from '@mustache-react/client';
import { useState } from 'react';
import Header from '../../components/Header';
import type { PageProps } from '../../generated/types';

// Props come from the Java method annotated @ReactPage("orders/OrderList"); see generated/types.ts.
// Texts come from messages.properties / messages_en.properties (mustache-react.context.messages=orders.*).
export default function OrderList({ orders, total }: PageProps<'orders/OrderList'>) {
  const { t } = useMessages();
  const [query, setQuery] = useState('');
  const visible = orders.filter((order) => order.customer.toLowerCase().includes(query.toLowerCase()));

  return (
    <main className="page">
      <Header />
      <h1>{t('orders.title')}</h1>
      <p>
        <a href="/orders/new">{t('orders.new')}</a>
      </p>
      <input
        aria-label={t('orders.searchLabel')}
        placeholder={t('orders.search')}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      <p data-testid="summary">{t('orders.summary', visible.length, total)}</p>
      <table>
        <thead>
          <tr>
            <th>{t('orders.column.id')}</th>
            <th>{t('orders.column.customer')}</th>
            <th>{t('orders.column.amount')}</th>
            <th>{t('orders.column.status')}</th>
            <th>{t('orders.column.orderedAt')}</th>
          </tr>
        </thead>
        <tbody>
          {visible.map((order) => (
            <tr key={order.id}>
              <td>
                <a href={`/orders/${order.id}`}>{order.id}</a>
              </td>
              <td>{order.customer}</td>
              <td>{t('orders.amount', order.amount)}</td>
              <td>{t(`orders.status.${order.status}`)}</td>
              <td>{order.orderedAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}
