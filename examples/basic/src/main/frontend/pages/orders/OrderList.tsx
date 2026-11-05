import { useState } from 'react';
import type { OrderListResponse } from '../../types';

export default function OrderList({ orders, total }: OrderListResponse) {
  const [query, setQuery] = useState('');
  const visible = orders.filter((order) => order.customer.toLowerCase().includes(query.toLowerCase()));

  return (
    <main className="page">
      <h1>주문 목록</h1>
      <input
        aria-label="고객 검색"
        placeholder="고객 이름으로 검색"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      <p data-testid="summary">
        {visible.length} / {total}건
      </p>
      <table>
        <thead>
          <tr>
            <th>번호</th>
            <th>고객</th>
            <th>금액</th>
            <th>상태</th>
            <th>주문일</th>
          </tr>
        </thead>
        <tbody>
          {visible.map((order) => (
            <tr key={order.id}>
              <td>{order.id}</td>
              <td>{order.customer}</td>
              <td>{order.amount.toLocaleString('ko-KR')}원</td>
              <td>{order.status}</td>
              <td>{order.orderedAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}
