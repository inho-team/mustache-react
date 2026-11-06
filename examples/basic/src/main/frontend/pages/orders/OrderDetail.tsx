import Header from '../../components/Header';
import type { PageProps } from '../../generated/types';

/** The controller answers 404 with order = null when there is no such order. */
export default function OrderDetail({ id, order }: PageProps<'orders/OrderDetail'>) {
  return (
    <main className="page narrow">
      <Header />
      <h1>주문 #{id}</h1>
      {order ? (
        <dl data-testid="order">
          <dt>고객</dt>
          <dd>{order.customer}</dd>
          <dt>금액</dt>
          <dd>{order.amount.toLocaleString('ko-KR')}원</dd>
          <dt>상태</dt>
          <dd>{order.status}</dd>
          <dt>주문일</dt>
          <dd>{order.orderedAt}</dd>
        </dl>
      ) : (
        <p className="error">주문을 찾을 수 없습니다.</p>
      )}
      <p>
        <a href="/orders">목록으로</a>
      </p>
    </main>
  );
}
