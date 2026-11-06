import { useSpringForm } from '@mustache-react/client';
import Header from '../../components/Header';
import type { OrderForm as Form, PageProps } from '../../generated/types';

/**
 * Posts to the @ReactPage handler at /orders/new. Validation errors from the BindingResult appear next to the
 * fields; "redirect:/orders" navigates.
 */
export default function OrderForm({ form: initial }: PageProps<'orders/OrderForm'>) {
  const form = useSpringForm<Form>({ initialValues: initial, action: '/orders/new' });
  return (
    <main className="page narrow">
      <Header />
      <h1>주문 등록</h1>
      <form onSubmit={form.submit} className="stack" noValidate>
        <label>
          고객
          <input {...form.field('customer')} />
          {form.errors.customer?.map((error) => (
            <span key={error} className="error" data-testid="customer-error">
              {error}
            </span>
          ))}
        </label>
        <label>
          금액
          <input type="number" {...form.field('amount')} />
          {form.errors.amount?.map((error) => (
            <span key={error} className="error" data-testid="amount-error">
              {error}
            </span>
          ))}
        </label>
        <button type="submit" disabled={form.submitting}>
          등록
        </button>
      </form>
    </main>
  );
}
