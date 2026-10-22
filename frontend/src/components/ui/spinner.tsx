// Loading simples: um círculo com um pedaço mais forte, girando.
// border-current usa a cor do texto do elemento pai, então serve em qualquer fundo.
export function Spinner({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`inline-block animate-spin rounded-full border-[3px] border-current border-r-transparent ${className}`}
    />
  );
}
