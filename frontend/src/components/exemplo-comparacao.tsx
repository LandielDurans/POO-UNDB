// Exemplo fixo (sem API) que mostra a ideia central do app:
// o posto mais barato nem sempre é o que compensa, porque ir até ele também gasta combustível.
// Conta: custo total = litros × preço + (distância ida e volta ÷ consumo) × preço.

const LITROS = 40;
const CONSUMO_KM_L = 12;

const postos = [
  { nome: "Posto Avenida", preco: 5.79, distanciaKm: 1.2 },
  { nome: "Posto Rodovia", preco: 5.59, distanciaKm: 14 },
];

const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

function custoTotal(preco: number, distanciaKm: number) {
  const litrosNoCaminho = (distanciaKm * 2) / CONSUMO_KM_L;
  return (LITROS + litrosNoCaminho) * preco;
}

export function ExemploComparacao() {
  const resultados = postos.map((p) => ({ ...p, total: custoTotal(p.preco, p.distanciaKm) }));
  const melhor = resultados.reduce((a, b) => (a.total <= b.total ? a : b));
  const economia = Math.max(...resultados.map((r) => r.total)) - melhor.total;

  return (
    <figure className="rounded-3xl bg-surface p-5 ring-1 ring-line sm:p-6">
      <figcaption className="flex items-baseline justify-between gap-4">
        <span className="font-display text-lg font-bold text-ink">Exemplo</span>
        <span className="text-sm text-muted">
          {LITROS} L · carro faz {CONSUMO_KM_L} km/L
        </span>
      </figcaption>

      <ul className="mt-4 flex flex-col gap-3">
        {resultados.map((r) => {
          const ehMelhor = r === melhor;
          return (
            <li
              key={r.nome}
              className={`rounded-2xl p-4 ${ehMelhor ? "bg-brand-soft ring-2 ring-brand" : "bg-sunk"}`}
            >
              <div className="flex items-center justify-between gap-3">
                <span className="font-semibold text-ink">{r.nome}</span>
                {ehMelhor ? (
                  <span className="rounded-full bg-brand px-3 py-1 text-xs font-bold text-on-brand">
                    Compensa mais
                  </span>
                ) : null}
              </div>
              <dl className="mt-3 grid grid-cols-3 gap-2 text-sm">
                <div>
                  <dt className="text-muted">Litro</dt>
                  <dd className="font-semibold tabular-nums text-ink">{brl.format(r.preco)}</dd>
                </div>
                <div>
                  <dt className="text-muted">Distância</dt>
                  <dd className="font-semibold tabular-nums text-ink">
                    {r.distanciaKm.toLocaleString("pt-BR")} km
                  </dd>
                </div>
                <div>
                  <dt className="text-muted">Custo real</dt>
                  <dd className="font-display text-base font-bold tabular-nums text-ink">
                    {brl.format(r.total)}
                  </dd>
                </div>
              </dl>
            </li>
          );
        })}
      </ul>

      <p className="mt-4 text-sm text-ink">
        O litro mais barato fica lá longe. Indo no <strong>{melhor.nome}</strong>, sobram{" "}
        <strong className="rounded bg-lane/60 px-1 tabular-nums">{brl.format(economia)}</strong> no
        seu bolso.
      </p>
    </figure>
  );
}
