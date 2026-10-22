import Link from "next/link";
import { ExemploComparacao } from "@/components/exemplo-comparacao";
import { Intro } from "@/components/intro";
import { Logo } from "@/components/logo";
import { primaryButton } from "@/components/ui/button";

const passos = [
  { titulo: "Fala qual é o seu carro", texto: "Quantos km ele faz com um litro." },
  { titulo: "Veja os postos por perto", texto: "Preço do dia e a distância até você." },
  { titulo: "Abasteça onde compensa", texto: "A gente soma o preço da bomba e o caminho até lá." },
];

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <Intro />
      <header className="mx-auto flex max-w-5xl items-center justify-between px-4 pt-6">
        <Logo />
        <Link
          href="/cadastro"
          className="rounded-full px-4 py-2 text-sm font-semibold text-brand-strong transition-colors hover:bg-brand-soft focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          Criar conta
        </Link>
      </header>

      <main className="mx-auto grid max-w-5xl gap-12 px-4 pb-16 pt-10 lg:grid-cols-[1.1fr_1fr] lg:items-center lg:gap-16 lg:pt-20">
        <section>
          <h1 className="font-display text-[clamp(2.25rem,7vw,3.75rem)] font-extrabold leading-[1.05] tracking-tight text-ink">
            Encha o tanque sem esvaziar o bolso.
          </h1>
          <p className="mt-5 max-w-[34ch] text-lg text-muted">
            Nem sempre o posto mais barato é o que vale a pena. A gente faz a conta e te mostra onde
            compensa de verdade.
          </p>
          <div className="mt-8 flex flex-wrap items-center gap-4">
            <Link href="/cadastro" className={primaryButton}>
              Bora começar
            </Link>
            <span className="text-sm text-muted">É rapidinho, menos de 1 minuto.</span>
          </div>
        </section>

        <ExemploComparacao />
      </main>

      <section className="mx-auto max-w-5xl px-4 pb-20" aria-labelledby="como-funciona">
        <div className="lane-line w-24" aria-hidden="true" />
        <h2 id="como-funciona" className="mt-6 font-display text-2xl font-bold text-ink">
          Como funciona
        </h2>
        <ol className="mt-6 grid gap-6 sm:grid-cols-3">
          {passos.map((passo, i) => (
            <li key={passo.titulo} className="flex gap-4">
              <span className="font-display text-3xl font-extrabold leading-none text-brand">
                {i + 1}
              </span>
              <div>
                <p className="font-semibold text-ink">{passo.titulo}</p>
                <p className="mt-1 text-sm text-muted">{passo.texto}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}
