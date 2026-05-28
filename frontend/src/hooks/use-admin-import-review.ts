"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { adminImportService } from "@/services/admin-import.service";
import type {
  ReviewOfficialValidationPayload,
  ReviewQuestionFilters,
  ReviewQuestionStatusPayload,
} from "@/types/question";

export function useReviewQuestions(filters: ReviewQuestionFilters) {
  return useQuery({
    queryKey: ["admin-review-questions", filters],
    queryFn: () => adminImportService.listReviewQuestions(filters),
  });
}

export function useReviewQuestion(questionId: number | null) {
  return useQuery({
    queryKey: ["admin-review-question", questionId],
    queryFn: () => adminImportService.getReviewQuestion(questionId as number),
    enabled: Number.isFinite(questionId),
  });
}

export function useUpdateReviewStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ReviewQuestionStatusPayload }) =>
      adminImportService.updateReviewStatus(id, payload),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      toast.success("Status de revisao atualizado.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Nao foi possivel atualizar o status da questao.")),
  });
}

export function useValidateOfficialSource() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: number;
      payload: ReviewOfficialValidationPayload;
    }) => adminImportService.validateOfficialSource(id, payload),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      toast.success("Questao marcada como validada na fonte oficial.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Nao foi possivel validar a fonte oficial.")),
  });
}

export function usePublishReviewQuestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.publishReviewQuestion(id),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Questao publicada com sucesso.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Nao foi possivel publicar a questao.")),
  });
}
