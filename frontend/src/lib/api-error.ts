import { AxiosError } from "axios";

import type { ApiResponse } from "@/types/api";

export interface AppError extends Error {
  status?: number;
  fieldErrors?: Record<string, string>;
}

function extractFieldErrors(metadata: unknown) {
  if (!metadata || typeof metadata !== "object" || Array.isArray(metadata)) {
    return undefined;
  }

  return Object.entries(metadata).reduce<Record<string, string>>((acc, entry) => {
    const [key, value] = entry;
    if (typeof value === "string") {
      acc[key] = value;
    }
    return acc;
  }, {});
}

function createAppError(message: string, status?: number, metadata?: unknown) {
  const appError = new Error(message) as AppError;
  appError.status = status;
  appError.fieldErrors = extractFieldErrors(metadata);
  return appError;
}

function resolveNetworkMessage(error: AxiosError) {
  if (error.code === "ECONNABORTED") {
    return "A API demorou para responder. Se o backend estiver iniciando no Render, aguarde alguns segundos e tente novamente.";
  }

  return "NÃ£o foi possÃ­vel alcanÃ§ar a API. Verifique se o backend no Render estÃ¡ ativo e se o domÃ­nio da Vercel estÃ¡ liberado no CORS.";
}

export function toAppError(error: unknown): AppError {
  if (error instanceof Error && "status" in error) {
    return error as AppError;
  }

  if (error instanceof AxiosError) {
    const response = error.response?.data as ApiResponse<unknown> | undefined;

    if (!error.response) {
      return createAppError(resolveNetworkMessage(error));
    }

    if (response?.message) {
      return createAppError(response.message, error.response.status, response.metadata);
    }

    if (error.response.status >= 500) {
      return createAppError("A API retornou um erro interno. Tente novamente em instantes.", error.response.status);
    }

    if (error.response.status === 401) {
      return createAppError("Credenciais invÃ¡lidas ou sessÃ£o expirada.", error.response.status);
    }

    return createAppError(
      "A API respondeu em um formato inesperado.",
      error.response.status,
      response?.metadata,
    );
  }

  return createAppError("Erro inesperado.");
}

export function getErrorMessage(error: unknown, fallback: string) {
  const appError = toAppError(error);
  return appError.message || fallback;
}
