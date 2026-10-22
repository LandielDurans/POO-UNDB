import type { Metadata } from "next";
import { Suspense } from "react";
import { AuthShell } from "@/components/auth-shell";
import { VerifyEmail } from "./verify-email";

export const metadata: Metadata = { title: "Confirmar e-mail · FuelFinder" };

// Suspense é exigido pelo Next quando um Client Component lê useSearchParams.
export default function VerificarEmailPage() {
  return (
    <AuthShell title="Confirmar e-mail" subtitle="Ativando sua conta.">
      <Suspense fallback={<p className="text-sm text-muted">Carregando...</p>}>
        <VerifyEmail />
      </Suspense>
    </AuthShell>
  );
}
