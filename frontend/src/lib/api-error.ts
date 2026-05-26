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

export function toAppError(error: unknown): AppError {
  if (error instanceof Error && "status" in error) {
    return error as AppError;
  }

  if (error instanceof AxiosError) {
    const response = error.response?.data as ApiResponse<unknown> | undefined;
    const appError = new Error(
      response?.message || error.message || "Não foi possível concluir a operação.",
    ) as AppError;

    appError.status = error.response?.status;
    appError.fieldErrors = extractFieldErrors(response?.metadata);
    return appError;
  }

  const fallback = new Error("Erro inesperado.") as AppError;
  return fallback;
}

export function getErrorMessage(error: unknown, fallback: string) {
  const appError = toAppError(error);
  return appError.message || fallback;
}
