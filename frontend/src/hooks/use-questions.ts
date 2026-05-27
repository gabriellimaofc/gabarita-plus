"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { questionsService } from "@/services/questions.service";
import type {
  AnswerQuestionPayload,
  ErrorNotebookFilters,
  QuestionFilters,
  UpdateErrorNotebookStatusPayload,
} from "@/types/question";

export function useQuestions(filters: QuestionFilters) {
  return useQuery({
    queryKey: ["questions", filters],
    queryFn: () => questionsService.list(filters),
  });
}

export function useQuestion(questionId: number) {
  return useQuery({
    queryKey: ["question", questionId],
    queryFn: () => questionsService.getById(questionId),
    enabled: Number.isFinite(questionId),
  });
}

export function useAnswerQuestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AnswerQuestionPayload) => questionsService.answer(payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      queryClient.invalidateQueries({ queryKey: ["question", data.questionId] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["statistics"] });
      queryClient.invalidateQueries({ queryKey: ["error-notebook"] });
      toast.success(
        data.correct ? "Resposta correta. Excelente." : "Resposta enviada. Vamos revisar.",
      );
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Nao foi possivel registrar sua resposta.")),
  });
}

export function useToggleFavorite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (questionId: number) => questionsService.toggleFavorite(questionId),
    onSuccess: (question) => {
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      queryClient.invalidateQueries({ queryKey: ["question", question.id] });
      queryClient.invalidateQueries({ queryKey: ["error-notebook"] });
      toast.success("Favoritos atualizados.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Nao foi possivel atualizar favoritos.")),
  });
}

export function useErrorNotebook(filters?: ErrorNotebookFilters) {
  return useQuery({
    queryKey: ["error-notebook", filters],
    queryFn: () => questionsService.getErrorNotebook(filters),
  });
}

export function useUpdateErrorNotebookStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      questionId,
      payload,
    }: {
      questionId: number;
      payload: UpdateErrorNotebookStatusPayload;
    }) => questionsService.updateErrorNotebookStatus(questionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["error-notebook"] });
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Status de dominio atualizado.");
    },
    onError: (error) =>
      toast.error(
        getErrorMessage(error, "Nao foi possivel atualizar o status no caderno de erros."),
      ),
  });
}
