import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";

import { toAppError } from "@/lib/api-error";
import { API_BASE_URL } from "@/lib/constants";
import { getSafeRedirectTarget } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse } from "@/types/auth";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 60_000,
  headers: {
    "Content-Type": "application/json",
  },
});

export const publicApiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 60_000,
  headers: {
    "Content-Type": "application/json",
  },
});

let refreshPromise: Promise<AuthResponse> | null = null;

export interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

function isApiResponse<T>(value: unknown): value is ApiResponse<T> {
  return Boolean(
    value &&
      typeof value === "object" &&
      "success" in value &&
      typeof (value as ApiResponse<T>).success === "boolean" &&
      "message" in value,
  );
}

export async function refreshSessionRequest(refreshToken: string) {
  const response = await publicApiClient.post<ApiResponse<AuthResponse>>("/auth/refresh", {
    refreshToken,
  });

  return unwrapResponse(response);
}

apiClient.interceptors.request.use((config) => {
  if (config.url?.startsWith("/auth/")) {
    return config;
  }

  const token = useAuthStore.getState().accessToken;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const { refreshToken, clearSession, setSession } = useAuthStore.getState();
    const status = (error as AxiosError).response?.status;
    const requestUrl = originalRequest?.url ?? "";
    const isAuthRequest = requestUrl.startsWith("/auth/");

    if (
      status === 401 &&
      originalRequest &&
      refreshToken &&
      !originalRequest._retry &&
      !isAuthRequest
    ) {
      originalRequest._retry = true;

      refreshPromise ??= refreshSessionRequest(refreshToken).finally(() => {
        refreshPromise = null;
      });

      try {
        const session = await refreshPromise;
        setSession(session);
        originalRequest.headers.Authorization = `Bearer ${session.accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        clearSession();
        if (typeof window !== "undefined") {
          const currentPath = `${window.location.pathname}${window.location.search}`;
          const redirectTarget = getSafeRedirectTarget(currentPath);
          window.location.href = `/login?redirectTo=${encodeURIComponent(redirectTarget)}`;
        }
        return Promise.reject(toAppError(refreshError));
      }
    }

    return Promise.reject(toAppError(error));
  },
);

export function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>) {
  if (!isApiResponse<T>(response.data)) {
    throw toAppError(
      new AxiosError("A API respondeu em formato inesperado.", undefined, response.config, undefined, response),
    );
  }

  return response.data.data;
}
