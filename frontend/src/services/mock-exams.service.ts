import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type {
  CreateMockExamPayload,
  FinishMockExamPayload,
  MockExam,
} from "@/types/mock-exam";

export const mockExamsService = {
  async list() {
    const response = await apiClient.get<ApiResponse<MockExam[]>>("/mock-exams");
    return unwrapResponse(response);
  },

  async create(payload: CreateMockExamPayload) {
    const response = await apiClient.post<ApiResponse<MockExam>>(
      "/mock-exams",
      payload,
    );
    return unwrapResponse(response);
  },

  async finish(mockExamId: number, payload: FinishMockExamPayload) {
    const response = await apiClient.post<ApiResponse<MockExam>>(
      `/mock-exams/${mockExamId}/finish`,
      payload,
    );
    return unwrapResponse(response);
  },
};
