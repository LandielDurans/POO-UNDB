# Guia de estudo — Front-end FuelFinder

Material para explicar o front-end na apresentação. Uso de IA: o Claude (Anthropic)
foi usado para gerar e explicar o código; todas as decisões abaixo foram revisadas.

## 1. Visão geral

O front é uma aplicação **Next.js 14** que conversa com o backend **Spring Boot** por HTTP/JSON.
O contrato entre os dois é o arquivo `docs/openapi/identity-v1.yaml`.

Fluxo do cadastro:

1. Usuário preenche `/cadastro`.
2. O front valida os campos (mesmas regras do backend).
3. O front envia `POST /api/v1/auth/register`.
4. O Next repassa a chamada para o backend (proxy em `next.config.mjs`).
5. Backend responde `202` (ok), `400` (dados inválidos) ou `409` (e-mail já existe).
6. O usuário recebe um e-mail com link para `/verificar-email?token=...`.
7. Essa tela envia o token para `POST /api/v1/auth/verify-email`. Resposta `204` = conta ativa.

## 2. Decisões e porquês

| Decisão | Por quê | Alternativa descartada |
| --- | --- | --- |
| Next.js App Router | Rotas por pasta, Server Components por padrão | Pages Router (modelo antigo) |
| TypeScript | Erros de tipo aparecem antes de rodar | JavaScript puro |
| Tailwind | Estilo junto do componente, escala de espaçamento fixa | CSS separado por arquivo |
| Proxy (`rewrites`) | Navegador fala só com o Next, sem precisar de CORS no Spring | Configurar CORS no backend |
| Tipos em `src/types` | Espelham o OpenAPI; se o contrato muda, o TypeScript acusa | Usar `any` |
| Validação no front | Resposta instantânea ao usuário | Só validar no backend |
| Cores como tokens (`brand`, `ink`) | Trocar tema sem mexer nos componentes | Cores fixas em cada classe |

## 2.1 Design (detalhes em `.impeccable.md`)

| Decisão | Por quê |
| --- | --- |
| Tema claro | Motorista usa no celular, de dia, muitas vezes no sol: claro tem mais contraste |
| Verde-jade + amarelo-manteiga | Cor própria e amigável, fugindo do laranja/vermelho de posto e do azul genérico. O amarelo lembra faixa de estrada |
| Cores em OKLCH | Passos iguais de luminosidade parecem iguais ao olho (HSL não garante isso) |
| Fonte Rubik (uma só) | Arredondada e amigável, no estilo do Waze. A fonte do Waze (Boing) é paga, a Rubik é gratuita |
| Botões e campos de 48px | Tamanho confortável para o polegar |
| Campo com fonte de 16px | Abaixo disso, o iPhone dá zoom sozinho ao tocar |
| Card "Exemplo" na home | Explica a ideia do app sem texto longo: o litro mais barato longe pode sair mais caro |
| Erro com ícone e texto | Nunca só cor (daltônicos também entendem) |
| Logo: gota com rostinho | Simpática como o Waze, mas própria. Feita em SVG (vetor): nítida em qualquer tamanho. Gradiente dá volume; `useId` cria ids únicos para os gradientes, porque a logo aparece mais de uma vez na página |
| Textos "abrasileirados" | Tom de conversa ("Bora começar", "sobram R$ 3,89 no seu bolso"): combina com a marca amigável |
| Intro animada (`intro.tsx`) | Abertura de 2 s, completa em toda abertura da home: a gota cai, quica e o nome aparece. Depois esmaece em 0,4 s. Toque pula a intro |
| Spinner (`ui/spinner.tsx`) | Loading girando simples: círculo com um lado transparente + `animate-spin` do Tailwind. Usado na intro e no botão enquanto envia |
| Troca de página sem animação | Os links usam `next/link` puro: a página troca direto. Simples para a 1ª entrega |
| Intro respeita "reduzir movimento" | Quem ativou isso no celular (enjoo, labirintite) não vê animação. Quem esconde é o CSS, então não pisca |
| Animação só com `transform` e `opacity` | A placa de vídeo anima direto, sem recalcular a página: fica lisa até em celular simples |

Bug que apareceu e foi corrigido: a primeira versão mostrava a intro só 1x por sessão
(salvo no `sessionStorage`). Ao recarregar, o servidor desenhava a intro e o React a escondia
logo em seguida, então ela piscava e sumia. Solução: rodar sempre a animação completa.
Lição: o que depende do navegador (storage) só existe depois que o React carrega,
e o servidor não sabe disso ao desenhar a página.

Conta do exemplo (`exemplo-comparacao.tsx`):
`custo real = (litros + km ida e volta ÷ consumo) × preço`.
Posto Avenida: (40 + 2,4 ÷ 12) × 5,79 = R$ 232,76.
Posto Rodovia: (40 + 28 ÷ 12) × 5,59 = R$ 236,64.

## 3. Conceitos que podem cair

- **Server Component x Client Component:** por padrão, componente roda no servidor.
  `"use client"` no topo do arquivo é necessário quando usa estado (`useState`),
  efeitos (`useEffect`) ou eventos (`onClick`). Por isso `page.tsx` é servidor e
  `register-form.tsx` é cliente.
- **`useState`:** guarda um valor que, ao mudar, faz o componente desenhar de novo.
- **`useEffect`:** roda código depois que o componente aparece na tela. Usado para
  enviar o token assim que a página de verificação abre.
- **`useRef` em `verify-email.tsx`:** em desenvolvimento o React roda efeitos duas vezes.
  O ref lembra que já enviou o token e evita gastar o token duas vezes.
- **`Suspense`:** o Next exige quando um Client Component lê `useSearchParams`
  (parâmetros da URL), para poder pré-renderizar o resto da página.
- **`event.preventDefault()`:** impede o formulário de recarregar a página.
- **Status HTTP:** `202` aceito, `204` sucesso sem corpo, `400` requisição inválida,
  `409` conflito (e-mail repetido), `0` no nosso código = falha de rede.
- **Problem Details (RFC 9457):** formato padrão de erro JSON com `title`, `status`, `detail`.
- **Classe `ApiError`:** estende `Error` (herança, POO) e carrega o status HTTP,
  para a tela escolher a mensagem certa.

## 4. Ligação com POO e o backend

- O backend usa **Arquitetura Hexagonal**: domínio puro no centro, adaptadores em volta.
  O front é mais um **adaptador externo**: só conhece o contrato HTTP, não as classes Java.
- A regra de senha (15 a 128 caracteres) vem de `PasswordPolicy.java`.
  O front repete a regra em `src/lib/validation.ts` para dar resposta rápida,
  mas o backend continua sendo a autoridade.
- `Array.from(senha).length` conta caracteres como o `codePointCount` do Java
  (um emoji conta como 1, não 2).

## 5. Acessibilidade

- Todo campo tem `<label>` ligado por `htmlFor`/`id`.
- Erro do campo ligado por `aria-describedby` e marcado com `aria-invalid`.
- Mensagens de sucesso/erro usam `role="alert"` para o leitor de tela anunciar.
- Botão mostra "Enviando..." e fica desabilitado durante o envio (evita clique duplo).

## 6. Pendências

- Login e sessão: dependem do backend.
- Reenviar verificação: endpoint planejado, ainda não implementado no backend.
- Contexto `station` (postos e cálculo de economia): próximo passo do front.
- CI do front (lint + build) no GitHub Actions.
