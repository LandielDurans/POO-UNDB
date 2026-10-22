import { Logo } from "@/components/logo";

// Moldura comum das telas de conta. No celular ocupa a tela toda;
// no desktop vira um painel centralizado.
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-md items-center px-4 pt-6 sm:pt-10">
        <Logo />
      </header>
      <main className="mx-auto max-w-md px-4 pb-12 pt-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-ink">{title}</h1>
        <p className="mt-2 text-base text-muted">{subtitle}</p>
        <div className="lane-line mt-6 w-16" aria-hidden="true" />
        <div className="mt-8">{children}</div>
      </main>
    </div>
  );
}
