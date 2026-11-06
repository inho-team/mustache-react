import { useCallback, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { serverContext, springFetch, type ValidationErrors } from './context.js';

export interface SpringFormOptions<T> {
  /** Initial values, usually the form object from the page props. */
  initialValues: T;
  /** URL the form is submitted to. Default: the current page. */
  action?: string;
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
  const initialErrors = serverContext().errors;
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
    const { action, method = 'POST', onSuccess, navigate } = optionsRef.current;
    setSubmitting(true);
    try {
      const response = await springFetch(action ?? window.location.pathname + window.location.search, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(valuesRef.current),
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
