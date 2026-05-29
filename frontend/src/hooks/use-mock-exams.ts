"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { mockExamsService } from "@/services/mock-exams.service";
import type {
  CreateMockExamPayload,
  FinishMockExamPayload,
  SaveMockExamAnswerPayload,
} from "@/types/mock-exam";

export function useMockExams() {
  return useQuery({
    queryKey: ["mock-exams"],
    queryFn: () => mockExamsService.list(),
  });
}

export function useMockExam(mockExamId: number) {
  return useQuery({
    queryKey: ["mock-exam", mockExamId],
    queryFn: () => mockExamsService.getById(mockExamId),
    enabled: Number.isFinite(mockExamId),
  });
}

export function useMockExamQuestions(mockExamId: number, enabled = true) {
  return useQuery({
    queryKey: ["mock-exam-questions", mockExamId],
    queryFn: () => mockExamsService.getQuestions(mockExamId),
    enabled: Number.isFinite(mockExamId) && enabled,
  });
}

export function useMockExamResult(mockExamId: number, enabled = true) {
  return useQuery({
    queryKey: ["mock-exam-result", mockExamId],
    queryFn: () => mockExamsService.getResult(mockExamId),
    enabled: Number.isFinite(mockExamId) && enabled,
  });
}

export function useCreateMockExam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateMockExamPayload) => mockExamsService.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["mock-exams"] });
      toast.success("Simulado criado com sucesso.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível criar o simulado.")),
  });
}

export function useSaveMockExamAnswer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      mockExamId,
      payload,
    }: {
      mockExamId: number;
      payload: SaveMockExamAnswerPayload;
    }) => mockExamsService.answer(mockExamId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["mock-exam", variables.mockExamId] });
      queryClient.invalidateQueries({
        queryKey: ["mock-exam-questions", variables.mockExamId],
      });
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível salvar a resposta do simulado.")),
  });
}

export function useFinishMockExam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      mockExamId,
      payload,
    }: {
      mockExamId: number;
      payload: FinishMockExamPayload;
    }) => mockExamsService.finish(mockExamId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["mock-exams"] });
      queryClient.invalidateQueries({ queryKey: ["mock-exam", variables.mockExamId] });
      queryClient.invalidateQueries({
        queryKey: ["mock-exam-questions", variables.mockExamId],
      });
      queryClient.invalidateQueries({
        queryKey: ["mock-exam-result", variables.mockExamId],
      });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["error-notebook"] });
      queryClient.invalidateQueries({ queryKey: ["questions"] });
      toast.success("Simulado finalizado com sucesso.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível finalizar o simulado.")),
  });
}
