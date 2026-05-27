import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type {
  CreateMockExamPayload,
  FinishMockExamPayload,
  MockExam,
  MockExamAnswerResult,
  MockExamQuestionDetail,
  MockExamResult,
  SaveMockExamAnswerPayload,
} from "@/types/mock-exam";

export const mockExamsService = {
  async list() {
    const response = await apiClient.get<ApiResponse<MockExam[]>>("/mock-exams");
    return unwrapResponse(response);
  },

  async getById(mockExamId: number) {
    const response = await apiClient.get<ApiResponse<MockExam>>(`/mock-exams/${mockExamId}`);
    return unwrapResponse(response);
  },

  async getQuestions(mockExamId: number) {
    const response = await apiClient.get<ApiResponse<MockExamQuestionDetail[]>>(
      `/mock-exams/${mockExamId}/questions`,
    );
    return unwrapResponse(response);
  },

  async getResult(mockExamId: number) {
    const response = await apiClient.get<ApiResponse<MockExamResult>>(
      `/mock-exams/${mockExamId}/result`,
    );
    return unwrapResponse(response);
  },

  async create(payload: CreateMockExamPayload) {
    const response = await apiClient.post<ApiResponse<MockExam>>(
      "/mock-exams",
      payload,
    );
    return unwrapResponse(response);
  },

  async answer(mockExamId: number, payload: SaveMockExamAnswerPayload) {
    const response = await apiClient.post<ApiResponse<MockExamAnswerResult>>(
      `/mock-exams/${mockExamId}/answers`,
      payload,
    );
    return unwrapResponse(response);
  },

  async finish(mockExamId: number, payload: FinishMockExamPayload) {
    const response = await apiClient.post<ApiResponse<MockExamResult>>(
      `/mock-exams/${mockExamId}/finish`,
      payload,
    );
    return unwrapResponse(response);
  },
};
