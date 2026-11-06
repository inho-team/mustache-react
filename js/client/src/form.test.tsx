// @vitest-environment jsdom
import { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { resetServerContext } from './context.js';
import { useSpringForm, type SpringForm } from './form.js';

(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;

interface OrderForm {
  customer: string;
  amount: number | null;
}

let root: Root;
let form: SpringForm<OrderForm>;
const navigate = vi.fn();

function Form() {
  form = useSpringForm<OrderForm>({ initialValues: { customer: '', amount: 0 }, action: '/orders/new', navigate });
  return (
    <form onSubmit={form.submit}>
      <input aria-label="customer" {...form.field('customer')} />
      <input aria-label="amount" {...form.field('amount')} />
    </form>
  );
}

function render(context?: unknown) {
  document.body.innerHTML =
    (context ? `<script type="application/json" id="mr-context">${JSON.stringify(context)}</script>` : '') + '<div id="app"></div>';
  resetServerContext();
  root = createRoot(document.getElementById('app')!);
  act(() => root.render(<Form />));
}

function respond(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

beforeEach(() => navigate.mockReset());
afterEach(() => {
  act(() => root.unmount());
  vi.unstubAllGlobals();
});

describe('useSpringForm', () => {
  it('binds inputs and keeps numbers numeric', () => {
    render();
    act(() => form.field('customer').onChange({ target: { value: '김민수' } } as never));
    act(() => form.field('amount').onChange({ target: { value: '1200' } } as never));

    expect(form.values).toEqual({ customer: '김민수', amount: 1200 });
    expect(form.field('amount').value).toBe('1200');
  });

  it('treats an empty type="number" input as a number field', () => {
    render();
    act(() => form.field('amount').onChange({ target: { value: '' } } as never));
    act(() => form.field('customer').onChange({ target: { value: '7', type: 'number' } } as never));

    expect(form.values).toEqual({ customer: 7, amount: null });
  });

  it('posts JSON with the CSRF token and shows @Valid errors', async () => {
    render({ csrf: { headerName: 'X-CSRF-TOKEN', parameterName: '_csrf', token: 'abc' } });
    const fetchMock = respond(400, { errors: { fields: { customer: ['must not be blank'] }, global: ['check the order'] } });

    await act(() => form.submit());

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/orders/new');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual({ customer: '', amount: 0 });
    expect(new Headers(init.headers).get('X-CSRF-TOKEN')).toBe('abc');
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json');
    expect(form.errors.customer).toEqual(['must not be blank']);
    expect(form.globalErrors).toEqual(['check the order']);
    expect(form.field('customer')['aria-invalid']).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('follows redirect: and clears errors', async () => {
    render({ errors: { fields: { customer: ['from the server'] }, global: [] } });
    expect(form.errors.customer).toEqual(['from the server']);
    respond(200, { redirect: '/orders' });

    await act(() => form.submit());

    expect(navigate).toHaveBeenCalledWith('/orders');
    expect(form.errors).toEqual({});
  });

  it('rejects unexpected responses', async () => {
    render();
    respond(500, {});

    await expect(act(() => form.submit())).rejects.toThrow('POST /orders/new failed with HTTP 500');
    expect(form.submitting).toBe(false);
  });
});
