# FuelFinder — Front-end

Next.js 14 (App Router) + TypeScript + Tailwind CSS.

## Rodar

```bash
cd frontend
npm install
npm run dev
```

Abre em http://localhost:3000. O backend precisa estar em http://localhost:8080
(ou defina `BACKEND_URL`, veja `.env.example`).

## Telas

| Rota | O que faz | Endpoint |
| --- | --- | --- |
| `/` | Página inicial | — |
| `/cadastro` | Cria conta local | `POST /api/v1/auth/register` |
| `/verificar-email?token=...` | Ativa a conta pelo link do e-mail | `POST /api/v1/auth/verify-email` |

## Estrutura

```text
src/
├── app/            # Rotas (cada pasta = uma URL)
├── components/     # Componentes reutilizáveis (ui/ = botão, campo, alerta)
├── lib/            # Chamadas à API e validações
└── types/          # Tipos espelhados do contrato OpenAPI
```

Guia de estudo das decisões: `docs/frontend/guia-de-estudo.md`.
