"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { mockExamsService } from "@/services/mock-exams.service";
import type {
  CreateMockExamPayload,
  FinishMockExamPayload,
} from "@/types/mock-exam";

export function useMockExams() {
  return useQuery({
    queryKey: ["mock-exams"],
    queryFn: () => mockExamsService.list(),
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["mock-exams"] });
      toast.success("Simulado finalizado com sucesso.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível finalizar o simulado.")),
  });
}
