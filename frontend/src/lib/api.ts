import type { Problem } from "@/types/identity";

// Erro da API com status HTTP, para a tela decidir a mensagem certa.
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem?: Problem,
  ) {
    super(problem?.detail ?? problem?.title ?? `Erro HTTP ${status}`);
  }
}

// POST com JSON. Respostas sem corpo (204) devolvem undefined.
export async function postJson<TResponse>(path: string, body: unknown): Promise<TResponse> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    // Falha de rede: backend fora do ar ou sem conexão.
    throw new ApiError(0);
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => undefined);
    throw new ApiError(response.status, problem);
  }

  if (response.status === 204) return undefined as TResponse;
  return response.json() as Promise<TResponse>;
}
