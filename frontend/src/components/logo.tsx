import Link from "next/link";
import { LogoMark } from "@/components/logo-mark";

// Logo completa: símbolo + nome. "Finder" em verde liga o nome à cor da marca.
export function Logo() {
  return (
    <Link
      href="/"
      aria-label="FuelFinder, página inicial"
      className="inline-flex items-center gap-2 rounded-full text-xl font-bold tracking-tight text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
    >
      <LogoMark className="h-9 w-9" />
      <span>
        Fuel<span className="text-brand">Finder</span>
      </span>
    </Link>
  );
}
