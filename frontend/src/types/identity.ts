// Tipos espelhados do contrato docs/openapi/identity-v1.yaml.
// Se o contrato mudar, estes tipos mudam junto.

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  userId: string;
  status: "PENDING_VERIFICATION";
}

export interface VerifyEmailRequest {
  token: string;
}

// Formato de erro RFC 9457 (Problem Details) devolvido pelo backend.
export interface Problem {
  type?: string;
  title: string;
  status: number;
  detail?: string;
}
