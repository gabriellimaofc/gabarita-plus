"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { adminImportService } from "@/services/admin-import.service";
import type {
  OfficialExamSourcePayload,
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

export function useReviewCounters() {
  return useQuery({
    queryKey: ["admin-review-counters"],
    queryFn: () => adminImportService.getReviewCounters(),
  });
}

export function useOfficialSources() {
  return useQuery({
    queryKey: ["admin-official-sources"],
    queryFn: () => adminImportService.listOfficialSources(),
  });
}

export function useCreateOfficialSource() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: OfficialExamSourcePayload) => adminImportService.createOfficialSource(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-official-sources"] });
      toast.success("Fonte oficial cadastrada.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível cadastrar a fonte oficial.")),
  });
}

export function useDeleteOfficialSource() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.deleteOfficialSource(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-official-sources"] });
      toast.success("Fonte oficial removida.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível remover a fonte oficial.")),
  });
}

export function useUpdateReviewStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ReviewQuestionStatusPayload }) =>
      adminImportService.updateReviewStatus(id, payload),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      toast.success("Status de revisão atualizado.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível atualizar o status da questão.")),
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
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      toast.success("Questão marcada como validada na fonte oficial.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível validar a fonte oficial.")),
  });
}

export function usePublishReviewQuestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.publishReviewQuestion(id),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Questão publicada com sucesso.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível publicar a questão.")),
  });
}

export function useAutoValidateQuestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.autoValidateQuestion(id),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.setQueryData(["admin-review-question", question.id], question);
      toast.success("Auto validação concluída.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível auto validar a questão.")),
  });
}

export function useAutoValidateBatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminImportService.autoValidateBatch(),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      toast.success(`Auto validação concluída: ${result.processed} questões processadas.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível auto validar o lote.")),
  });
}

export function useAutoPublishSafe() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminImportService.autoPublishSafe(),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success(`Publicação segura processada: ${result.published} questões publicadas.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível publicar questões seguras.")),
  });
}

export function useRecoverAssets() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.recoverAssets(id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-question"] });
      toast.success(`Recuperação processada: ${result.assetRecovered} assets recuperados.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível recuperar assets com INEP.")),
  });
}

export function useRecoverAssetsBatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminImportService.recoverAssetsBatch(),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-question"] });
      toast.success(`Recuperação em lote processada: ${result.processed} questões.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível recuperar assets do lote.")),
  });
}

export function useValidateAgainstOfficialSource() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => adminImportService.validateAgainstOfficialSource(id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-question"] });
      toast.success(`Validação INEP: ${result.validated} validadas, ${result.failed} falhas.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível validar com INEP.")),
  });
}

export function useValidateAgainstOfficialSourceBatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminImportService.validateAgainstOfficialSourceBatch(),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-review-questions"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-counters"] });
      queryClient.invalidateQueries({ queryKey: ["admin-review-question"] });
      toast.success(`Validação INEP em lote: ${result.validated} validadas, ${result.failed} falhas.`);
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível validar o lote com INEP.")),
  });
}
