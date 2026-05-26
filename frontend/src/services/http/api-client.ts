import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";

import { toAppError } from "@/lib/api-error";
import { API_BASE_URL } from "@/lib/constants";
import { useAuthStore } from "@/store/auth-store";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse } from "@/types/auth";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

const authClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let refreshPromise: Promise<AuthResponse> | null = null;

export interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

export async function refreshSessionRequest(refreshToken: string) {
  const response = await authClient.post<ApiResponse<AuthResponse>>(
    "/auth/refresh",
    { refreshToken },
  );

  return response.data.data;
}

apiClient.interceptors.request.use((config) => {
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
          window.location.href = "/login";
        }
        return Promise.reject(toAppError(refreshError));
      }
    }

    return Promise.reject(toAppError(error));
  },
);

export function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>) {
  return response.data.data;
}
