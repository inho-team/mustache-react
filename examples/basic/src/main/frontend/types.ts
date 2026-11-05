// Mirrors the Java DTOs in run.iknow.mustachereact.examples.basic.orders.
// From M3 this file is generated from the Java classes.

export type OrderStatus = 'PAID' | 'SHIPPED' | 'CANCELLED';

export interface Order {
  id: number;
  customer: string;
  amount: number;
  status: OrderStatus;
  orderedAt: string;
}

export interface OrderListResponse {
  orders: Order[];
  total: number;
}
