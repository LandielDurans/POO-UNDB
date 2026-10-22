import { useId } from "react";

// Símbolo da marca: uma gota de combustível simpática, com volume e brilho.
// Desenhado em SVG (vetor): fica nítido em qualquer tamanho.
// useId gera ids únicos para os gradientes, já que a logo aparece mais de uma vez na página.
export function LogoMark({ className = "" }: { className?: string }) {
  const id = useId();
  const corpo = `${id}-corpo`;
  const luz = `${id}-luz`;

  return (
    <svg viewBox="0 0 48 48" aria-hidden="true" className={className}>
      <defs>
        {/* Verde mais claro em cima e mais escuro embaixo: dá a sensação de volume */}
        <linearGradient id={corpo} x1="0.2" y1="0" x2="0.8" y2="1">
          <stop offset="0" stopColor="oklch(0.66 0.13 160)" />
          <stop offset="1" stopColor="oklch(0.42 0.09 165)" />
        </linearGradient>
        {/* Luz suave no centro da gota */}
        <radialGradient id={luz} cx="0.4" cy="0.55" r="0.5">
          <stop offset="0" stopColor="white" stopOpacity="0.22" />
          <stop offset="1" stopColor="white" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* Corpo da gota */}
      <path
        fill={`url(#${corpo})`}
        d="M24 3.5c-.9 0-1.7.5-2.2 1.2C17.6 11 9 22.6 9 30.5a15 15 0 0 0 30 0C39 22.6 30.4 11 26.2 4.7c-.5-.7-1.3-1.2-2.2-1.2Z"
      />
      <path
        fill={`url(#${luz})`}
        d="M24 3.5c-.9 0-1.7.5-2.2 1.2C17.6 11 9 22.6 9 30.5a15 15 0 0 0 30 0C39 22.6 30.4 11 26.2 4.7c-.5-.7-1.3-1.2-2.2-1.2Z"
      />
      {/* Reflexo amarelo */}
      <path
        d="M17 24.5c1-3.3 2.8-6.6 4.9-9.7"
        fill="none"
        strokeWidth="2.6"
        strokeLinecap="round"
        className="stroke-lane"
      />
      {/* Olhinhos fechados e felizes */}
      <path
        d="M17.2 31c.8-1.6 3.4-1.6 4.2 0M26.6 31c.8-1.6 3.4-1.6 4.2 0"
        fill="none"
        strokeWidth="2.2"
        strokeLinecap="round"
        className="stroke-on-brand"
      />
      {/* Sorriso */}
      <path
        d="M19.6 35.4c2.5 2.4 6.3 2.4 8.8 0"
        fill="none"
        strokeWidth="2.4"
        strokeLinecap="round"
        className="stroke-on-brand"
      />
    </svg>
  );
}
