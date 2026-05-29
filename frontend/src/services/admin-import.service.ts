import { DEFAULT_PAGE_SIZE } from "@/lib/constants";
import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse, PageMetadata } from "@/types/api";
import type {
  AutoValidationBatchResult,
  AutoValidationCounters,
  OfficialExamSource,
  OfficialExamSourcePayload,
  OfficialValidationReport,
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
    autoValidationStatus: filters.autoValidationStatus || undefined,
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

  async autoValidateQuestion(id: number) {
    const response = await apiClient.post<ApiResponse<ReviewQuestionDetail>>(
      `/admin/import/questions/${id}/auto-validate`,
    );
    return unwrapResponse(response);
  },

  async autoValidateBatch() {
    const response = await apiClient.post<ApiResponse<AutoValidationBatchResult>>(
      "/admin/import/questions/auto-validate-batch",
    );
    return unwrapResponse(response);
  },

  async autoPublishSafe() {
    const response = await apiClient.post<ApiResponse<AutoValidationBatchResult>>(
      "/admin/import/questions/auto-publish-safe",
    );
    return unwrapResponse(response);
  },

  async getReviewCounters() {
    const response = await apiClient.get<ApiResponse<AutoValidationCounters>>(
      "/admin/import/questions/review/counters",
    );
    return unwrapResponse(response);
  },

  async listOfficialSources() {
    const response = await apiClient.get<ApiResponse<OfficialExamSource[]>>(
      "/admin/import/official-sources",
    );
    return unwrapResponse(response);
  },

  async createOfficialSource(payload: OfficialExamSourcePayload) {
    const response = await apiClient.post<ApiResponse<OfficialExamSource>>(
      "/admin/import/official-sources",
      payload,
    );
    return unwrapResponse(response);
  },

  async recoverAssets(id: number) {
    const response = await apiClient.post<ApiResponse<OfficialValidationReport>>(
      `/admin/import/questions/${id}/recover-assets`,
    );
    return unwrapResponse(response);
  },

  async recoverAssetsBatch() {
    const response = await apiClient.post<ApiResponse<OfficialValidationReport>>(
      "/admin/import/questions/recover-assets-batch",
    );
    return unwrapResponse(response);
  },

  async validateAgainstOfficialSource(id: number) {
    const response = await apiClient.post<ApiResponse<OfficialValidationReport>>(
      `/admin/import/questions/${id}/validate-against-official-source`,
    );
    return unwrapResponse(response);
  },

  async validateAgainstOfficialSourceBatch() {
    const response = await apiClient.post<ApiResponse<OfficialValidationReport>>(
      "/admin/import/questions/validate-against-official-source-batch",
    );
    return unwrapResponse(response);
  },
};
