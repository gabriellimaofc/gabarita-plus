"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { Clock3, FileSpreadsheet, Trophy } from "lucide-react";
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
import { useCreateMockExam, useMockExams } from "@/hooks/use-mock-exams";
import { formatDate, formatSecondsToMinutes } from "@/lib/utils";

const createSchema = z.object({
  title: z.string().min(3, "Informe um título."),
  durationMinutes: z.coerce.number().min(30, "Use pelo menos 30 minutos."),
  questionCount: z.coerce.number().min(10, "Use pelo menos 10 questões."),
});

type CreateMockExamFormValues = z.infer<typeof createSchema>;

export function MockExamsView() {
  const router = useRouter();
  const { data, isLoading, isError, refetch } = useMockExams();
  const createMockExam = useCreateMockExam();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateMockExamFormValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      title: "",
      durationMinutes: 90,
      questionCount: 20,
    },
  });

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Simulados"
        title="Monte provas completas e resolva como se fosse o dia oficial."
        description="Crie simulados com seleção automática de questões, acompanhe o progresso, finalize com confirmação e consulte o resultado detalhado depois."
      />

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <CardHeader>
            <CardTitle>Seus simulados</CardTitle>
            <CardDescription>
              Acompanhe o que está em andamento, retome a resolução e compare o resultado final por matéria.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {isLoading
              ? Array.from({ length: 3 }).map((_, index) => (
                  <Skeleton key={index} className="h-40" />
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
                description="Crie seu primeiro bloco de prova e entre direto na resolução."
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
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div className="space-y-2">
                        <div className="flex flex-wrap gap-2">
                          <Badge variant={exam.finished ? "success" : "secondary"}>
                            {exam.finished ? "Finalizado" : "Em andamento"}
                          </Badge>
                          <Badge variant="outline">
                            <Clock3 className="mr-1 size-3.5" />
                            {exam.durationMinutes} min
                          </Badge>
                          <Badge variant="outline">
                            <FileSpreadsheet className="mr-1 size-3.5" />
                            {exam.questionCount} questões
                          </Badge>
                        </div>
                        <h2 className="text-lg font-semibold">{exam.title}</h2>
                        <p className="text-sm text-muted-foreground">
                          {exam.answeredCount} resposta(s) registradas, {exam.correctCount} acerto(s) e {exam.unansweredCount} pendente(s).
                        </p>
                      </div>

                      <div className="text-right text-sm text-muted-foreground">
                        <p>Criado em {formatDate(exam.createdAt)}</p>
                        {exam.finished && exam.timeSpentSeconds ? (
                          <p>Tempo total {formatSecondsToMinutes(exam.timeSpentSeconds)}</p>
                        ) : null}
                      </div>
                    </div>

                    <div className="mt-4 flex flex-col gap-3 rounded-[20px] bg-muted/30 p-4 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p className="text-sm text-muted-foreground">Nota final</p>
                        <p className="text-2xl font-semibold">
                          {exam.finalScore !== null ? exam.finalScore.toFixed(1) : "--"}
                        </p>
                      </div>

                      <div className="flex flex-wrap gap-2">
                        <Link href={`/simulados/${exam.id}`}>
                          <Button>{exam.finished ? "Ver resultado" : "Resolver simulado"}</Button>
                        </Link>
                      </div>
                    </div>
                  </div>
                ))
              : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Criar novo simulado</CardTitle>
            <CardDescription>
              O backend seleciona automaticamente a quantidade de questões pedida e abre o fluxo real de resolução.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form
              className="space-y-4"
              onSubmit={handleSubmit(async (values) => {
                try {
                  const exam = await createMockExam.mutateAsync({
                    ...values,
                  });

                  router.push(`/simulados/${exam.id}`);
                } catch {
                  return;
                }
              })}
            >
              <div className="space-y-2">
                <Label htmlFor="title">Titulo</Label>
                <Input id="title" {...register("title")} />
                {errors.title ? (
                  <p className="text-sm text-rose-500">{errors.title.message}</p>
                ) : null}
              </div>

              <div className="space-y-2">
                <Label htmlFor="durationMinutes">Duracao em minutos</Label>
                <Input id="durationMinutes" type="number" {...register("durationMinutes")} />
                {errors.durationMinutes ? (
                  <p className="text-sm text-rose-500">
                    {errors.durationMinutes.message}
                  </p>
                ) : null}
              </div>

              <div className="space-y-2">
                <Label htmlFor="questionCount">Quantidade de questões</Label>
                <Input id="questionCount" type="number" {...register("questionCount")} />
                {errors.questionCount ? (
                  <p className="text-sm text-rose-500">
                    {errors.questionCount.message}
                  </p>
                ) : null}
              </div>

              <div className="rounded-[24px] bg-muted/40 p-4 text-sm leading-7 text-muted-foreground">
                O sistema cria o caderno automaticamente, salva o progresso em tempo real e calcula o desempenho completo ao finalizar.
              </div>

              <Button type="submit" className="w-full" disabled={createMockExam.isPending}>
                {createMockExam.isPending ? "Criando..." : "Criar e comecar agora"}
              </Button>
            </form>

            <div className="mt-6 rounded-[24px] border border-border/70 bg-background/70 p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-2xl bg-primary/10 p-3 text-primary">
                  <Trophy className="size-5" />
                </div>
                <div>
                  <p className="font-semibold">Experiencia de prova</p>
                  <p className="text-sm text-muted-foreground">
                    Resolução em sequência, cronômetro, cartão-resposta e resultado por matéria.
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
