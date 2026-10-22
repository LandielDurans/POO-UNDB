"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { registerUser } from "@/lib/identity-api";
import { PASSWORD_MIN, validateEmail, validateName, validatePassword } from "@/lib/validation";

type Errors = Partial<Record<"name" | "email" | "password", string | null>>;
type Status = "idle" | "submitting" | "success";

export function RegisterForm() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Errors>({});
  const [status, setStatus] = useState<Status>("idle");
  const [serverError, setServerError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); // impede o recarregamento padrão do formulário
    setServerError(null);

    const nextErrors: Errors = {
      name: validateName(name),
      email: validateEmail(email),
      password: validatePassword(password),
    };
    setErrors(nextErrors);
    if (Object.values(nextErrors).some(Boolean)) return;

    setStatus("submitting");
    try {
      await registerUser({ name: name.trim(), email: email.trim(), password });
      setStatus("success");
    } catch (error) {
      setStatus("idle");
      setServerError(messageFor(error));
    }
  }

  if (status === "success") {
    return (
      <Alert tone="success" title="Conta criada!">
        Enviamos um link de confirmação para <strong>{email}</strong>. Abra o e-mail para ativar
        sua conta.
      </Alert>
    );
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      {serverError ? (
        <Alert tone="error" title="Não foi possível criar a conta">
          {serverError}
        </Alert>
      ) : null}

      <Field
        id="name"
        label="Nome"
        autoComplete="name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        error={errors.name}
      />
      <Field
        id="email"
        label="E-mail"
        type="email"
        autoComplete="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={errors.email}
      />
      <Field
        id="password"
        label="Senha"
        type="password"
        autoComplete="new-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        error={errors.password}
        hint={`Mínimo de ${PASSWORD_MIN} caracteres. Uma frase funciona bem.`}
      />

      <Button type="submit" loading={status === "submitting"} className="mt-2">
        Criar conta
      </Button>
    </form>
  );
}

// Traduz o status HTTP do contrato em mensagem para o usuário.
function messageFor(error: unknown): string {
  if (!(error instanceof ApiError)) return "Erro inesperado. Tente novamente.";
  if (error.status === 0) return "Sem conexão com o servidor. Verifique se o backend está rodando.";
  if (error.status === 409) return "Este e-mail já está cadastrado.";
  if (error.status === 400) return error.problem?.detail ?? "Confira os dados e tente novamente.";
  return "O servidor falhou. Tente de novo em instantes.";
}
