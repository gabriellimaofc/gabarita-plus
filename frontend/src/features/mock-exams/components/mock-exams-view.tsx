"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useCreateMockExam, useFinishMockExam, useMockExams } from "@/hooks/use-mock-exams";
import { useQuestions } from "@/hooks/use-questions";

const createSchema = z.object({
  title: z.string().min(3, "Informe um título."),
  durationMinutes: z.coerce.number().min(30, "Use pelo menos 30 minutos."),
});

type CreateMockExamFormValues = z.infer<typeof createSchema>;

export function MockExamsView() {
  const [finalScores, setFinalScores] = useState<Record<number, string>>({});
  const { data, isLoading, isError, refetch } = useMockExams();
  const {
    data: availableQuestions,
    isLoading: isLoadingQuestions,
  } = useQuestions({ page: 0, size: 20, direction: "DESC" });
  const { mutate, isPending } = useCreateMockExam();
  const { mutate: finishMockExam, isPending: isFinishing } = useFinishMockExam();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateMockExamFormValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      title: "",
      durationMinutes: 90,
    },
  });

  const questionIds = availableQuestions?.items.map((question) => question.id) ?? [];

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Simulados"
        title="Monte blocos de treino com contexto de prova."
        description="Crie simulados temáticos, acompanhe conclusão e use o histórico para ajustar seu ritmo."
      />

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <CardHeader>
            <CardTitle>Seus simulados</CardTitle>
            <CardDescription>
              Acompanhe status, duração e nota final dos ciclos mais recentes.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {isLoading
              ? Array.from({ length: 3 }).map((_, index) => (
                  <Skeleton key={index} className="h-32" />
                ))
              : null}

            {isError ? (
              <ErrorState
                title="Não foi possível carregar os simulados."
                description="Tente novamente para sincronizar a lista com o backend."
                onRetry={() => void refetch()}
              />
            ) : null}

            {!isLoading && !isError && data?.length === 0 ? (
              <EmptyState
                title="Nenhum simulado criado ainda."
                description="Monte seu primeiro bloco usando as questões já disponíveis na plataforma."
              />
            ) : null}

            {!isLoading &&
            !isError &&
            data?.length
              ? data.map((exam) => (
                  <div
                    key={exam.id}
                    className="rounded-[24px] border border-border/70 bg-background/70 p-5"
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="space-y-2">
                        <h2 className="text-lg font-semibold">{exam.title}</h2>
                        <p className="text-sm text-muted-foreground">
                          {exam.questions.length} questões • {exam.durationMinutes} minutos
                        </p>
                      </div>
                      <Badge variant={exam.finished ? "success" : "secondary"}>
                        {exam.finished ? "Concluído" : "Em andamento"}
                      </Badge>
                    </div>
                    <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                      <span>
                        Nota final:{" "}
                        <strong className="text-foreground">
                          {exam.finalScore ? exam.finalScore.toFixed(1) : "Pendente"}
                        </strong>
                      </span>
                      <span>{new Date(exam.createdAt).toLocaleDateString("pt-BR")}</span>
                    </div>

                    {!exam.finished ? (
                      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                        <Input
                          type="number"
                          step="0.1"
                          placeholder="Nota final"
                          value={finalScores[exam.id] ?? ""}
                          onChange={(event) =>
                            setFinalScores((current) => ({
                              ...current,
                              [exam.id]: event.target.value,
                            }))
                          }
                        />
                        <Button
                          type="button"
                          disabled={
                            isFinishing ||
                            !finalScores[exam.id] ||
                            Number(finalScores[exam.id]) < 0
                          }
                          onClick={() =>
                            finishMockExam({
                              mockExamId: exam.id,
                              payload: {
                                finalScore: Number(finalScores[exam.id]),
                              },
                            })
                          }
                        >
                          {isFinishing ? "Finalizando..." : "Finalizar"}
                        </Button>
                      </div>
                    ) : null}
                  </div>
                ))
              : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Criar novo simulado</CardTitle>
            <CardDescription>
              O envio usa o contrato real do backend com `title`, `durationMinutes` e `questionIds`.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form
              className="space-y-4"
              onSubmit={handleSubmit((values) =>
                mutate({ ...values, questionIds }),
              )}
            >
              <div className="space-y-2">
                <Label htmlFor="title">Título</Label>
                <Input id="title" {...register("title")} />
                {errors.title ? (
                  <p className="text-sm text-rose-500">{errors.title.message}</p>
                ) : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="durationMinutes">Duração em minutos</Label>
                <Input id="durationMinutes" type="number" {...register("durationMinutes")} />
                {errors.durationMinutes ? (
                  <p className="text-sm text-rose-500">
                    {errors.durationMinutes.message}
                  </p>
                ) : null}
              </div>
              <div className="rounded-[24px] bg-muted/40 p-4 text-sm leading-7 text-muted-foreground">
                {isLoadingQuestions
                  ? "Carregando questões disponíveis para o novo simulado..."
                  : questionIds.length > 0
                    ? `${questionIds.length} questões disponíveis serão enviadas neste simulado.`
                    : "Nenhuma questão disponível no momento para montar o simulado."}
              </div>
              <Button
                type="submit"
                className="w-full"
                disabled={isPending || isLoadingQuestions || questionIds.length === 0}
              >
                {isPending ? "Criando..." : "Criar simulado"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
