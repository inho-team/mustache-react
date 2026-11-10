import { useCallback, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { springFetch, useServerContext, type ValidationErrors } from './context.js';

export interface SpringFormOptions<T> {
  /** Initial values, usually the form object from the page props. */
  initialValues: T;
  /** URL the form is submitted to. Default: the current page. */
  action?: string;
  /**
   * How the values are sent. `json` (default) is for `@RequestBody` parameters; `form` sends
   * `application/x-www-form-urlencoded`, as an HTML form does, for `@ModelAttribute` parameters. Nested objects
   * become `address.city`, lists `items[0].name`, as Spring's data binder expects.
   */
  encoding?: 'json' | 'form';
  /** HTTP method. Default: `POST`. */
  method?: 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  /** Called with the JSON body after a successful submission that does not redirect. */
  onSuccess?: (body: unknown) => void;
  /** Navigates after `return "redirect:/path"`. Default: `window.location.assign`. */
  navigate?: (location: string) => void;
}

/** Props for an `<input>`, `<select>` or `<textarea>`. */
export interface FieldProps {
  name: string;
  value: string;
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => void;
  'aria-invalid'?: boolean;
}

export interface SpringForm<T> {
  values: T;
  setValue: <K extends keyof T>(name: K, value: T[K]) => void;
  /** Binds an input: `<input {...form.field('customer')} />`. Numbers in `initialValues` stay numbers. */
  field: (name: keyof T & string) => FieldProps;
  /** Field errors from `@Valid`, by field name. */
  errors: Partial<Record<keyof T & string, string[]>> & Record<string, string[]>;
  /** Errors not tied to a field (`BindingResult.reject`). */
  globalErrors: string[];
  submitting: boolean;
  /** `<form onSubmit={form.submit}>` */
  submit: (event?: FormEvent) => Promise<void>;
}

/**
 * Form state for a Spring MVC handler such as
 *
 * ```java
 * @ReactPage("orders/OrderForm")
 * @PostMapping("/orders/new")
 * public Object create(@Valid @RequestBody OrderForm form, BindingResult result) {
 *     if (result.hasErrors()) return new OrderFormPage(form);
 *     orders.add(form);
 *     return "redirect:/orders";
 * }
 * ```
 *
 * Submits the values as JSON with the CSRF token; shows the `@Valid` errors (400 responses) next to the fields;
 * follows `redirect:`. Errors rendered by the server (a page re-rendered after a plain form post) are shown from the
 * start.
 */
export function useSpringForm<T extends object>(options: SpringFormOptions<T>): SpringForm<T> {
  // From the provider during server-side rendering, from the page in the browser: both render the same errors.
  const initialErrors = useServerContext().errors;
  const [values, setValues] = useState<T>(options.initialValues);
  const [errors, setErrors] = useState<ValidationErrors>(initialErrors ?? { fields: {}, global: [] });
  const [submitting, setSubmitting] = useState(false);
  const optionsRef = useRef(options);
  optionsRef.current = options;
  const valuesRef = useRef(values);
  valuesRef.current = values;

  const setValue = useCallback(<K extends keyof T>(name: K, value: T[K]) => {
    setValues((current) => ({ ...current, [name]: value }));
  }, []);

  const field = (name: keyof T & string): FieldProps => {
    const value: unknown = values[name];
    const initial: unknown = options.initialValues[name];
    // A field starting empty (null) is numeric when the input is type="number"; see onChange.
    return {
      name,
      value: value === null || value === undefined ? '' : String(value),
      onChange: (event) => {
        const raw = event.target.value;
        const numeric = typeof initial === 'number' || (event.target as HTMLInputElement).type === 'number';
        setValue(name, (numeric ? (raw === '' ? null : Number(raw)) : raw) as T[typeof name]);
      },
      ...(errors.fields[name]?.length ? { 'aria-invalid': true } : {}),
    };
  };

  const submit = async (event?: FormEvent) => {
    event?.preventDefault();
    const { action, method = 'POST', encoding = 'json', onSuccess, navigate } = optionsRef.current;
    setSubmitting(true);
    try {
      const response = await springFetch(action ?? window.location.pathname + window.location.search, {
        method,
        ...(encoding === 'form'
          ? { body: toFormData(valuesRef.current) }
          : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(valuesRef.current) }),
      });
      const text = await response.text();
      const body: unknown = text ? JSON.parse(text) : null;
      const record = (body ?? {}) as { redirect?: string; errors?: ValidationErrors };
      if (response.status === 400 && record.errors) {
        setErrors({ fields: record.errors.fields ?? {}, global: record.errors.global ?? [] });
        return;
      }
      if (!response.ok) {
        throw new Error(`${method} ${action ?? 'form'} failed with HTTP ${response.status}`);
      }
      setErrors({ fields: {}, global: [] });
      if (record.redirect) {
        (navigate ?? ((location: string) => window.location.assign(location)))(record.redirect);
        return;
      }
      onSuccess?.(body);
    } finally {
      setSubmitting(false);
    }
  };

  return {
    values,
    setValue,
    field,
    errors: errors.fields as SpringForm<T>['errors'],
    globalErrors: errors.global,
    submitting,
    submit,
  };
}

/**
 * Form fields for Spring's data binder: `{address: {city}}` becomes `address.city`, `{items: [{name}]}` becomes
 * `items[0].name`, a list of plain values repeats the name. `null` and `undefined` become empty values, which the
 * binder turns into `null` for numbers.
 */
export function toFormData(values: object): URLSearchParams {
  const params = new URLSearchParams();
  const add = (name: string, value: unknown): void => {
    if (value === null || value === undefined) {
      params.append(name, '');
    } else if (Array.isArray(value)) {
      value.forEach((item, index) =>
        item !== null && typeof item === 'object' ? add(`${name}[${index}]`, item) : params.append(name, String(item)),
      );
    } else if (value instanceof Date) {
      params.append(name, value.toISOString());
    } else if (typeof value === 'object') {
      for (const [key, child] of Object.entries(value)) {
        add(name ? `${name}.${key}` : key, child);
      }
    } else {
      params.append(name, String(value));
    }
  };
  add('', values);
  return params;
}
