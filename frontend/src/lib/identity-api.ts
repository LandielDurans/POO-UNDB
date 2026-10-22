import { postJson } from "@/lib/api";
import type { RegisterRequest, RegisterResponse } from "@/types/identity";

// Uma função por endpoint do contexto identity.

export function registerUser(data: RegisterRequest) {
  return postJson<RegisterResponse>("/api/v1/auth/register", data);
}

export function verifyEmail(token: string) {
  return postJson<void>("/api/v1/auth/verify-email", { token });
}
