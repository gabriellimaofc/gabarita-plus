import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type { UpdateProfilePayload, UserProfile, UserStatistics } from "@/types/user";

export const userService = {
  async getProfile() {
    const response = await apiClient.get<ApiResponse<UserProfile>>("/users/me");
    return unwrapResponse(response);
  },

  async updateProfile(payload: UpdateProfilePayload) {
    const response = await apiClient.put<ApiResponse<UserProfile>>(
      "/users/me",
      payload,
    );
    return unwrapResponse(response);
  },

  async getStatistics() {
    const response = await apiClient.get<ApiResponse<UserStatistics>>(
      "/users/me/statistics",
    );
    return unwrapResponse(response);
  },
};
