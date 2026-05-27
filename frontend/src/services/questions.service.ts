import { DEFAULT_PAGE_SIZE } from "@/lib/constants";
import { apiClient, unwrapResponse } from "@/services/http/api-client";
import type { ApiResponse } from "@/types/api";
import type { PageMetadata } from "@/types/api";
import type {
  AnswerQuestionPayload,
  AnswerQuestionResult,
  ErrorNotebookEntry,
  ErrorNotebookFilters,
  Question,
  QuestionFilters,
  QuestionListResponse,
  UpdateErrorNotebookStatusPayload,
} from "@/types/question";

function buildQuestionParams(filters: QuestionFilters) {
  return {
    page: filters.page ?? 0,
    size: filters.size ?? DEFAULT_PAGE_SIZE,
    sortBy: filters.sortBy ?? "createdAt",
    direction: filters.direction ?? "DESC",
    search: filters.search || undefined,
    subject: filters.subject || undefined,
    topic: filters.topic || undefined,
    subtopic: filters.subtopic || undefined,
    difficulty: filters.difficulty || undefined,
    year: filters.year || undefined,
    exam: filters.exam || undefined,
    answered:
      filters.answered === "" || filters.answered === undefined
        ? undefined
        : filters.answered,
    incorrectOnly: filters.incorrectOnly || undefined,
    favoritesOnly: filters.favoritesOnly || undefined,
  };
}

function buildErrorNotebookParams(filters?: ErrorNotebookFilters) {
  if (!filters) {
    return undefined;
  }

  return {
    subject: filters.subject || undefined,
    topic: filters.topic || undefined,
    difficulty: filters.difficulty || undefined,
    masteryStatus: filters.masteryStatus || undefined,
    priority: filters.priority || undefined,
  };
}

export const questionsService = {
  async list(filters: QuestionFilters): Promise<QuestionListResponse> {
    const response = await apiClient.get<ApiResponse<Question[]>>("/questions", {
      params: buildQuestionParams(filters),
    });

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

  async getById(id: number): Promise<Question> {
    const response = await apiClient.get<ApiResponse<Question>>(`/questions/${id}`);
    return unwrapResponse(response);
  },

  async answer(payload: AnswerQuestionPayload): Promise<AnswerQuestionResult> {
    const response = await apiClient.post<ApiResponse<AnswerQuestionResult>>(
      "/questions/answers",
      payload,
    );
    return unwrapResponse(response);
  },

  async toggleFavorite(id: number) {
    const response = await apiClient.post<ApiResponse<Question>>(
      `/questions/${id}/favorite`,
    );
    return unwrapResponse(response);
  },

  async getErrorNotebook(filters?: ErrorNotebookFilters) {
    const response = await apiClient.get<ApiResponse<ErrorNotebookEntry[]>>(
      "/questions/error-notebook",
      {
        params: buildErrorNotebookParams(filters),
      },
    );
    return unwrapResponse(response);
  },

  async updateErrorNotebookStatus(
    questionId: number,
    payload: UpdateErrorNotebookStatusPayload,
  ) {
    const response = await apiClient.patch<ApiResponse<ErrorNotebookEntry>>(
      `/questions/error-notebook/${questionId}`,
      payload,
    );
    return unwrapResponse(response);
  },
};
