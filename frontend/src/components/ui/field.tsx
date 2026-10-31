import type { InputHTMLAttributes } from "react";

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  id: string;
  label: string;
  error?: string | null;
  hint?: string;
}

// Campo de formulário com label, dica e erro ligados por id (acessibilidade).
// text-base (16px) evita o zoom automático do iPhone ao tocar no campo.
export function Field({ id, label, error, hint, ...inputProps }: FieldProps) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-sm font-semibold text-ink">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className="h-12 rounded-2xl border border-line bg-surface px-4 text-base text-ink outline-none transition-colors placeholder:text-muted/70 focus:border-brand focus:ring-4 focus:ring-brand/15 aria-[invalid=true]:border-danger aria-[invalid=true]:focus:ring-danger/15"
        {...inputProps}
      />
      {error ? (
        <p id={`${id}-error`} className="flex items-start gap-1.5 text-sm font-medium text-danger">
          <span aria-hidden="true">!</span>
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-sm text-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
