"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, Circle } from "lucide-react";
import { startTransition, useEffect, useMemo, useState } from "react";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageShellSkeleton } from "@/components/common/page-shell-skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AnswerFeedback } from "@/features/questions/components/answer-feedback";
import { QuestionNavigator } from "@/features/questions/components/question-navigator";
import { QuestionProgress } from "@/features/questions/components/question-progress";
import {
  buildErrorNotebookParams,
  parseErrorNotebookFilters,
} from "@/features/errors/lib/error-notebook-filters";
import {
  useAnswerQuestion,
  useErrorNotebook,
  useQuestion,
  useUpdateErrorNotebookStatus,
} from "@/hooks/use-questions";
import { cn, formatDate } from "@/lib/utils";

export function ErrorNotebookReviewMode() {
  const router = useRouter();
  const pathname = usePathname();
  const rawSearchParams = useSearchParams();
  const searchParams = new URLSearchParams(rawSearchParams.toString());
  const filters = parseErrorNotebookFilters(searchParams);
  const { data: notebookEntries, isLoading, isError, refetch } = useErrorNotebook(filters);
  const [selectedAlternative, setSelectedAlternative] = useState<string | null>(null);
  const [startedAt, setStartedAt] = useState(Date.now());
  const currentIndex = Math.max(Number(searchParams.get("index") ?? "0"), 0);
  const { mutateAsync: answerQuestion, data: answerResult, isPending } = useAnswerQuestion();
  const { mutate: updateNotebookStatus, isPending: isUpdatingNotebookStatus } =
    useUpdateErrorNotebookStatus();

  const questionIds = notebookEntries?.map((entry) => entry.questionId) ?? [];
  const safeIndex =
    notebookEntries && notebookEntries.length > 0
      ? Math.min(currentIndex, notebookEntries.length - 1)
      : 0;
  const currentEntry = notebookEntries?.[safeIndex];
  const currentQuestionId = currentEntry?.questionId ?? 0;
  const {
    data: question,
    isLoading: isLoadingQuestion,
    isError: isQuestionError,
    refetch: refetchQuestion,
  } = useQuestion(currentQuestionId);

  useEffect(() => {
    setSelectedAlternative(null);
    setStartedAt(Date.now());
  }, [currentQuestionId]);

  const activeAnswer = useMemo(() => {
    if (answerResult?.questionId === currentQuestionId) {
      return answerResult;
    }

    if (question?.answered && question.correctAlternative) {
      return {
        questionId: currentQuestionId,
        correct: Boolean(question.answeredCorrectly),
        correctAlternative: question.correctAlternative,
        explanation: question.explanation,
      };
    }

    return null;
  }, [answerResult, currentQuestionId, question]);

  const updateIndex = (nextIndex: number) => {
    const params = buildErrorNotebookParams(filters);
    if (nextIndex > 0) {
      params.set("index", String(nextIndex));
    }

    startTransition(() => {
      router.replace(params.toString() ? `${pathname}?${params.toString()}` : pathname, {
        scroll: false,
      });
    });
  };

  const backHref = rawSearchParams.toString()
    ? `/caderno-erros?${buildErrorNotebookParams(filters).toString()}`
    : "/caderno-erros";

  if (isLoading) {
    return <PageShellSkeleton />;
  }

  if (isError) {
    return (
      <ErrorState
        title="Nao foi possivel abrir o modo revisao."
        description="Tente novamente para buscar a fila atualizada do caderno de erros."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!notebookEntries?.length || !currentEntry) {
    return (
      <EmptyState
        title="Nao ha questoes para revisar agora."
        description="Seu caderno de erros nao encontrou itens para este filtro de revisao."
        actionLabel="Voltar ao caderno"
        onAction={() => router.push("/caderno-erros")}
      />
    );
  }

  if (isLoadingQuestion) {
    return <PageShellSkeleton />;
  }

  if (isQuestionError || !question) {
    return (
      <ErrorState
        title="Nao foi possivel carregar a questao da revisao."
        description="Tente novamente para sincronizar a fila com os dados atuais do backend."
        onRetry={() => void refetchQuestion()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <QuestionNavigator
        currentIndex={safeIndex}
        totalCount={notebookEntries.length}
        backHref={backHref}
        onPrevious={() => updateIndex(Math.max(safeIndex - 1, 0))}
        onNext={() => updateIndex(Math.min(safeIndex + 1, notebookEntries.length - 1))}
        previousDisabled={safeIndex === 0}
        nextDisabled={safeIndex >= notebookEntries.length - 1}
        backLabel="Voltar para caderno"
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <QuestionProgress
            current={safeIndex + 1}
            total={notebookEntries.length}
            answeredCount={notebookEntries.length}
            label="Modo revisao"
          />

          <Card>
            <CardHeader className="space-y-4">
              <div className="flex flex-wrap gap-2">
                <Badge>{question.subject}</Badge>
                <Badge variant="secondary">{question.topic}</Badge>
                <Badge variant="outline">Fila de erros</Badge>
              </div>
              <CardTitle className="text-2xl">{question.title}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-8">
              <p className="text-sm leading-8 text-muted-foreground">{question.statement}</p>

              <div className="space-y-3">
                {question.alternatives.map((alternative) => {
                  const isSelected = selectedAlternative === alternative.letter;
                  const isCorrect =
                    activeAnswer?.correctAlternative !== null &&
                    alternative.letter === activeAnswer?.correctAlternative;

                  return (
                    <button
                      key={alternative.id}
                      type="button"
                      className={cn(
                        "flex w-full items-start gap-4 rounded-[24px] border px-5 py-4 text-left transition",
                        isSelected
                          ? "border-primary bg-primary/5"
                          : "border-border bg-background/60 hover:bg-accent",
                        activeAnswer && isCorrect && "border-emerald-500 bg-emerald-500/5",
                        activeAnswer &&
                          isSelected &&
                          !isCorrect &&
                          "border-rose-500 bg-rose-500/5",
                      )}
                      onClick={() => setSelectedAlternative(alternative.letter)}
                      disabled={Boolean(activeAnswer)}
                    >
                      <div className="mt-0.5">
                        {activeAnswer && isCorrect ? (
                          <CheckCircle2 className="size-5 text-emerald-500" />
                        ) : (
                          <Circle className={cn("size-5", isSelected && "text-primary")} />
                        )}
                      </div>
                      <div>
                        <p className="font-semibold">{alternative.letter}</p>
                        <p className="mt-1 text-sm leading-7 text-muted-foreground">
                          {alternative.text}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  Revise, receba o feedback e siga direto para a proxima questao da fila.
                </p>
                <Button
                  disabled={!selectedAlternative || Boolean(activeAnswer) || isPending}
                  onClick={() =>
                    answerQuestion({
                      questionId: question.id,
                      chosenAlternative: selectedAlternative ?? "",
                      timeSpentSeconds: Math.max(1, Math.floor((Date.now() - startedAt) / 1000)),
                    })
                  }
                >
                  {isPending ? "Enviando..." : "Responder"}
                </Button>
              </div>

              {activeAnswer ? (
                <AnswerFeedback
                  correct={activeAnswer.correct}
                  correctAlternative={activeAnswer.correctAlternative}
                  explanation={activeAnswer.explanation}
                  showMarkMasteredAction
                  isMarkingMastered={isUpdatingNotebookStatus}
                  onMarkMastered={() =>
                    updateNotebookStatus({
                      questionId: question.id,
                      payload: {
                        masteryStatus: "MASTERED",
                      },
                    })
                  }
                />
              ) : null}
            </CardContent>
          </Card>
        </div>

        <Card className="h-fit">
          <CardContent className="space-y-4 p-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
                Contexto da revisao
              </p>
              <p className="mt-1 text-lg font-semibold">{currentEntry.questionTitle}</p>
            </div>

            <div className="grid gap-3 text-sm text-muted-foreground">
              <div>
                <p className="font-medium text-foreground">Erros acumulados</p>
                <p>{currentEntry.errorCount}</p>
              </div>
              <div>
                <p className="font-medium text-foreground">Ultimo erro</p>
                <p>
                  {currentEntry.lastErrorAt
                    ? formatDate(currentEntry.lastErrorAt)
                    : "Sem registro"}
                </p>
              </div>
              <div>
                <p className="font-medium text-foreground">Ultima revisao</p>
                <p>
                  {currentEntry.lastReviewedAt
                    ? formatDate(currentEntry.lastReviewedAt)
                    : "Ainda nao revisada"}
                </p>
              </div>
              <div>
                <p className="font-medium text-foreground">Proxima revisao</p>
                <p>
                  {currentEntry.nextReviewAt
                    ? formatDate(currentEntry.nextReviewAt)
                    : "Sem agenda"}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
