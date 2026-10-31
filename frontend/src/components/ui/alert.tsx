interface AlertProps {
  tone: "success" | "error";
  title: string;
  children?: React.ReactNode;
}

// role="alert" faz o leitor de tela anunciar a mensagem assim que aparece.
// O ícone e o título dizem o tipo da mensagem, não só a cor.
export function Alert({ tone, title, children }: AlertProps) {
  const isSuccess = tone === "success";

  return (
    <div
      role="alert"
      className={`flex gap-3 rounded-2xl p-4 ${isSuccess ? "bg-brand-soft" : "bg-danger/10"}`}
    >
      <span
        aria-hidden="true"
        className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-sm font-bold text-on-brand ${isSuccess ? "bg-success" : "bg-danger"}`}
      >
        {isSuccess ? "✓" : "!"}
      </span>
      <div>
        <p className="font-semibold text-ink">{title}</p>
        {children ? <div className="mt-1 text-sm text-ink/80">{children}</div> : null}
      </div>
    </div>
  );
}
