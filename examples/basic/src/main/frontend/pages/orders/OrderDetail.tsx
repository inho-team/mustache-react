import { useMessages } from '@mustache-react/client';
import Header from '../../components/Header';
import type { PageProps } from '../../generated/types';

/** The controller answers 404 with order = null when there is no such order. */
export default function OrderDetail({ id, order }: PageProps<'orders/OrderDetail'>) {
  const { t } = useMessages();
  return (
    <main className="page narrow">
      <Header />
      <h1>{t('orders.detail.heading', String(id))}</h1>
      {order ? (
        <dl data-testid="order">
          <dt>{t('orders.column.customer')}</dt>
          <dd>{order.customer}</dd>
          <dt>{t('orders.column.amount')}</dt>
          <dd>{t('orders.amount', order.amount)}</dd>
          <dt>{t('orders.column.status')}</dt>
          <dd>{t(`orders.status.${order.status}`)}</dd>
          <dt>{t('orders.column.orderedAt')}</dt>
          <dd>{order.orderedAt}</dd>
        </dl>
      ) : (
        <p className="error">{t('orders.detail.notFound')}</p>
      )}
      <p>
        <a href="/orders">{t('orders.detail.back')}</a>
      </p>
    </main>
  );
}
