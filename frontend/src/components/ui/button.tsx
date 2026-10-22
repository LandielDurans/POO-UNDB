import type { ButtonHTMLAttributes } from "react";
import { Spinner } from "@/components/ui/spinner";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
}

// Classes do botão principal, exportadas para links que se parecem com botão.
export const primaryButton =
  "inline-flex h-12 items-center justify-center gap-2 rounded-full bg-brand px-6 text-base font-semibold text-on-brand transition-[background-color,transform] duration-150 ease-out hover:bg-brand-strong active:scale-[0.98] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand/30 disabled:cursor-not-allowed disabled:opacity-60";

export function Button({ loading, disabled, children, className = "", ...props }: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`${primaryButton} ${className}`}
      {...props}
    >
      {loading ? (
        <>
          <Spinner className="h-5 w-5" />
          Enviando...
        </>
      ) : (
        children
      )}
    </button>
  );
}
