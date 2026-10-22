"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Alert } from "@/components/ui/alert";
import { verifyEmail } from "@/lib/identity-api";

type Status = "verifying" | "success" | "invalid";

// O link do e-mail chega como /verificar-email?token=...
export function VerifyEmail() {
  const token = useSearchParams().get("token");
  const [status, setStatus] = useState<Status>(token ? "verifying" : "invalid");
  const sent = useRef(false); // evita enviar 2x (o React roda efeitos 2x em dev)

  useEffect(() => {
    if (!token || sent.current) return;
    sent.current = true;
    verifyEmail(token)
      .then(() => setStatus("success"))
      .catch(() => setStatus("invalid"));
  }, [token]);

  if (status === "verifying") {
    return (
      <p role="status" className="text-sm text-muted">
        Confirmando seu e-mail...
      </p>
    );
  }

  if (status === "success") {
    return (
      <Alert tone="success" title="E-mail confirmado!">
        Sua conta está ativa.{" "}
        <Link href="/" className="font-medium text-brand underline">
          Voltar ao início
        </Link>
      </Alert>
    );
  }

  return (
    <Alert tone="error" title="Link inválido ou expirado">
      Peça um novo link de confirmação ou{" "}
      <Link href="/cadastro" className="font-medium text-brand underline">
        faça o cadastro de novo
      </Link>
      .
    </Alert>
  );
}
