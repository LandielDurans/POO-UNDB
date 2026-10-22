"use client";

import { useEffect, useState } from "react";
import { LogoMark } from "@/components/logo-mark";
import { Spinner } from "@/components/ui/spinner";

// Tempo total na tela. Precisa bater com o fim da animação "intro-sair" em globals.css
// (começa em 2000ms e dura 400ms).
const DURACAO_MS = 2400;

// Tela de abertura: a gota cai, quica de leve e o nome aparece. Depois a intro sai.
// Roda completa toda vez que a home abre. Quem prefere menos movimento não vê
// (o CSS esconde, então não pisca na tela).
export function Intro() {
  const [visivel, setVisivel] = useState(true);

  useEffect(() => {
    // Depois do fade, tira a intro do HTML para não ficar uma camada invisível por cima.
    const timer = setTimeout(() => setVisivel(false), DURACAO_MS);
    return () => clearTimeout(timer);
  }, []);

  if (!visivel) return null;

  return (
    <div
      aria-hidden="true"
      onClick={() => setVisivel(false)}
      className="intro fixed inset-0 z-50 flex flex-col items-center justify-center gap-6"
    >
      <div className="intro-gota rounded-full bg-canvas p-8 shadow-[0_24px_48px_-12px_oklch(0.2_0.05_165/0.5)]">
        <LogoMark className="h-28 w-28" />
      </div>
      <p className="intro-nome mt-4 text-5xl font-bold tracking-tight text-on-brand">
        Fuel<span className="text-lane">Finder</span>
      </p>
      <p className="intro-frase -mt-2 text-xl text-on-brand/85">Bora economizar?</p>
      <div className="intro-frase mt-4 text-on-brand/80">
        <Spinner className="h-8 w-8" />
      </div>
    </div>
  );
}
