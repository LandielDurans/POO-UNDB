import type { Metadata, Viewport } from "next";
import { Rubik } from "next/font/google";
import "./globals.css";

// Uma fonte só, arredondada e amigável (estilo parecido com a do Waze, que é paga).
// O next/font baixa no build e serve do próprio site (sem pedir ao Google em cada acesso).
const rubik = Rubik({ subsets: ["latin"], variable: "--font-rubik" });

export const metadata: Metadata = {
  title: "FuelFinder",
  description: "Encontre o posto com melhor custo-benefício perto de você.",
};

export const viewport: Viewport = {
  themeColor: "#f7f5ec",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" className={rubik.variable}>
      <body>{children}</body>
    </html>
  );
}
