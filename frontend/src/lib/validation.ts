// Regras iguais às do backend (PasswordPolicy.java e RegisterRequest).
// O front valida para dar resposta rápida; o backend continua sendo a autoridade.

export const PASSWORD_MIN = 15;
export const PASSWORD_MAX = 128;

export function validateName(name: string): string | null {
  const trimmed = name.trim();
  if (trimmed.length < 2) return "Nome precisa ter pelo menos 2 caracteres.";
  if (trimmed.length > 100) return "Nome pode ter no máximo 100 caracteres.";
  return null;
}

export function validateEmail(email: string): string | null {
  if (email.length > 254) return "E-mail muito longo.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return "Digite um e-mail válido.";
  return null;
}

export function validatePassword(password: string): string | null {
  // Array.from conta caracteres reais (emoji conta 1), igual ao codePointCount do Java.
  const length = Array.from(password).length;
  if (password.trim().length === 0) return "Senha não pode ser vazia.";
  if (length < PASSWORD_MIN || length > PASSWORD_MAX) {
    return `Senha deve ter entre ${PASSWORD_MIN} e ${PASSWORD_MAX} caracteres.`;
  }
  return null;
}
