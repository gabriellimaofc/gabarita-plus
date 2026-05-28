import { DEFAULT_PAGE_SIZE } from "@/lib/constants";
import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse, PageMetadata } from "@/types/api";
import type {
  ReviewOfficialValidationPayload,
  ReviewQuestionDetail,
  ReviewQuestionFilters,
  ReviewQuestionListResponse,
  ReviewQuestionStatusPayload,
  ReviewQuestionSummary,
} from "@/types/question";

function buildReviewParams(filters: ReviewQuestionFilters) {
  return {
    page: filters.page ?? 0,
    size: filters.size ?? DEFAULT_PAGE_SIZE,
    sortBy: filters.sortBy ?? "createdAt",
    direction: filters.direction ?? "DESC",
    status: filters.status || undefined,
    source: filters.source || undefined,
    year: filters.year || undefined,
    subject: filters.subject || undefined,
  };
}

export const adminImportService = {
  async listReviewQuestions(filters: ReviewQuestionFilters): Promise<ReviewQuestionListResponse> {
    const response = await apiClient.get<ApiResponse<ReviewQuestionSummary[]>>(
      "/admin/import/questions/review",
      {
        params: buildReviewParams(filters),
      },
    );

    const metadata = response.data.metadata as PageMetadata | undefined;

    return {
      items: unwrapResponse(response),
      metadata: metadata ?? {
        page: filters.page ?? 0,
        size: filters.size ?? DEFAULT_PAGE_SIZE,
        totalElements: response.data.data.length,
        totalPages: 1,
        first: true,
        last: true,
      },
    };
  },

  async getReviewQuestion(id: number): Promise<ReviewQuestionDetail> {
    const response = await apiClient.get<ApiResponse<ReviewQuestionDetail>>(
      `/admin/import/questions/review/${id}`,
    );
    return unwrapResponse(response);
  },

  async updateReviewStatus(id: number, payload: ReviewQuestionStatusPayload) {
    const response = await apiClient.patch<ApiResponse<ReviewQuestionDetail>>(
      `/admin/import/questions/review/${id}/status`,
      payload,
    );
    return unwrapResponse(response);
  },

  async validateOfficialSource(id: number, payload: ReviewOfficialValidationPayload) {
    const response = await apiClient.patch<ApiResponse<ReviewQuestionDetail>>(
      `/admin/import/questions/review/${id}/validate-official-source`,
      payload,
    );
    return unwrapResponse(response);
  },

  async publishReviewQuestion(id: number) {
    const response = await apiClient.post<ApiResponse<ReviewQuestionDetail>>(
      `/admin/import/questions/review/${id}/publish`,
    );
    return unwrapResponse(response);
  },
};
