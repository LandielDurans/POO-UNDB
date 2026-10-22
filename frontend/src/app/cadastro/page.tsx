import type { Metadata } from "next";
import { AuthShell } from "@/components/auth-shell";
import { RegisterForm } from "./register-form";

export const metadata: Metadata = { title: "Criar conta · FuelFinder" };

// Server Component: só monta a página. A parte interativa fica no RegisterForm.
export default function CadastroPage() {
  return (
    <AuthShell title="Criar conta" subtitle="Cadastre seu carro e descubra onde abastecer gastando menos.">
      <RegisterForm />
    </AuthShell>
  );
}
