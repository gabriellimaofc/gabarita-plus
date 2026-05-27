import {
  apiClient,
  publicApiClient,
  refreshSessionRequest,
  unwrapResponse,
} from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
} from "@/types/auth";

export const authService = {
  async login(payload: LoginPayload) {
    const response = await publicApiClient.post<ApiResponse<AuthResponse>>(
      "/auth/login",
      payload,
    );
    return unwrapResponse(response);
  },

  async register(payload: RegisterPayload) {
    const response = await publicApiClient.post<ApiResponse<AuthResponse>>(
      "/auth/register",
      payload,
    );
    return unwrapResponse(response);
  },

  async refresh(refreshToken: string) {
    return refreshSessionRequest(refreshToken);
  },

  async logout(refreshToken: string) {
    await apiClient.post<ApiResponse<null>>("/auth/logout", { refreshToken });
  },
};
