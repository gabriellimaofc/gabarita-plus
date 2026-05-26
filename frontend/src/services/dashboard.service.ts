import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type { DashboardData } from "@/types/dashboard";

export const dashboardService = {
  async getDashboard() {
    const response = await apiClient.get<ApiResponse<DashboardData>>("/dashboard");
    return unwrapResponse(response);
  },
};
