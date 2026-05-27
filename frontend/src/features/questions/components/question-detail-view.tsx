"use client";

import { useSearchParams } from "next/navigation";
import { CheckCircle2, Circle, Heart, Timer } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { ErrorState } from "@/components/common/error-state";
import { PageShellSkeleton } from "@/components/common/page-shell-skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  AnswerFeedback,
} from "@/features/questions/components/answer-feedback";
import { QuestionNavigator } from "@/features/questions/components/question-navigator";
import { QuestionProgress } from "@/features/questions/components/question-progress";
import {
  useAnswerQuestion,
  useQuestion,
  useToggleFavorite,
  useUpdateErrorNotebookStatus,
} from "@/hooks/use-questions";
import { cn, getSafeRedirectTarget } from "@/lib/utils";

function buildQuestionHref({
  targetQuestionId,
  targetIndex,
  ids,
  returnTo,
  source,
}: {
  targetQuestionId: number;
  targetIndex: number;
  ids: number[];
  returnTo: string;
  source?: string | null;
}) {
  const params = new URLSearchParams({
    ids: ids.join(","),
    index: String(targetIndex),
    returnTo,
  });

  if (source) {
    params.set("source", source);
  }

  return `/questoes/${targetQuestionId}?${params.toString()}`;
}

export function QuestionDetailView({ questionId }: { questionId: number }) {
  const searchParams = useSearchParams();
  const { data: question, isLoading, isError, refetch } = useQuestion(questionId);
  const { mutateAsync: answerQuestion, data: answerResult, isPending } = useAnswerQuestion();
  const { mutate: toggleFavorite } = useToggleFavorite();
  const { mutate: updateNotebookStatus, isPending: isUpdatingNotebookStatus } =
    useUpdateErrorNotebookStatus();
  const [selectedAlternative, setSelectedAlternative] = useState<string | null>(null);
  const [startedAt, setStartedAt] = useState(Date.now());

  useEffect(() => {
    setSelectedAlternative(null);
    setStartedAt(Date.now());
  }, [questionId]);

  const source = searchParams.get("source");
  const ids = searchParams
    .get("ids")
    ?.split(",")
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value)) ?? [questionId];
  const questionIndexFromList = ids.findIndex((id) => id === questionId);
  const fallbackIndex = Number(searchParams.get("index") ?? "0");
  const currentIndex =
    questionIndexFromList >= 0
      ? questionIndexFromList
      : Number.isFinite(fallbackIndex)
        ? fallbackIndex
        : 0;
  const returnTo = getSafeRedirectTarget(searchParams.get("returnTo"));

  const previousQuestionId = currentIndex > 0 ? ids[currentIndex - 1] : undefined;
  const nextQuestionId =
    currentIndex < ids.length - 1 ? ids[currentIndex + 1] : undefined;

  const previousHref = previousQuestionId
    ? buildQuestionHref({
        targetQuestionId: previousQuestionId,
        targetIndex: currentIndex - 1,
        ids,
        returnTo,
        source,
      })
    : undefined;
  const nextHref = nextQuestionId
    ? buildQuestionHref({
        targetQuestionId: nextQuestionId,
        targetIndex: currentIndex + 1,
        ids,
        returnTo,
        source,
      })
    : undefined;

  const activeAnswer = useMemo(() => {
    if (answerResult?.questionId === questionId) {
      return answerResult;
    }

    if (question?.answered && question.correctAlternative) {
      return {
        questionId,
        correct: Boolean(question.answeredCorrectly),
        correctAlternative: question.correctAlternative,
        explanation: question.explanation,
      };
    }

    return null;
  }, [answerResult, question, questionId]);

  if (isLoading) {
    return <PageShellSkeleton />;
  }

  if (isError || !question) {
    return (
      <ErrorState
        title="Nao foi possivel carregar esta questao."
        description="Tente novamente para buscar os dados atualizados do backend."
        onRetry={() => void refetch()}
      />
    );
  }

  const revealedCorrectAlternative =
    activeAnswer?.correctAlternative ?? question.correctAlternative ?? null;
  const hasAnsweredCurrentQuestion = Boolean(activeAnswer);

  return (
    <div className="space-y-6">
      <QuestionNavigator
        currentIndex={currentIndex}
        totalCount={ids.length}
        backHref={returnTo}
        previousHref={previousHref}
        nextHref={nextHref}
      />

      <QuestionProgress
        current={currentIndex + 1}
        total={ids.length}
        label={source === "error-notebook" ? "Revisao guiada" : "Fluxo de questoes"}
      />

      <Card>
        <CardHeader className="space-y-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="space-y-3">
              <div className="flex flex-wrap gap-2">
                <Badge>{question.subject}</Badge>
                <Badge variant="secondary">{question.topic}</Badge>
                <Badge variant="outline">
                  <Timer className="mr-1 size-3.5" />
                  Ritmo monitorado
                </Badge>
                {question.answered ? (
                  <Badge variant={question.answeredCorrectly ? "success" : "warning"}>
                    {question.answeredCorrectly ? "Ja acertada" : "Ja revisada"}
                  </Badge>
                ) : null}
              </div>
              <CardTitle className="text-2xl">{question.title}</CardTitle>
            </div>

            <Button variant="outline" onClick={() => toggleFavorite(question.id)}>
              <Heart className={cn("size-4", question.favorite && "fill-current text-rose-500")} />
              Favoritar
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-8">
          <p className="text-sm leading-8 text-muted-foreground">{question.statement}</p>

          <div className="space-y-3">
            {question.alternatives.map((alternative) => {
              const isSelected = selectedAlternative === alternative.letter;
              const isCorrect =
                revealedCorrectAlternative !== null &&
                alternative.letter === revealedCorrectAlternative;

              return (
                <button
                  key={alternative.id}
                  type="button"
                  className={cn(
                    "flex w-full items-start gap-4 rounded-[24px] border px-5 py-4 text-left transition",
                    isSelected
                      ? "border-primary bg-primary/5"
                      : "border-border bg-background/60 hover:bg-accent",
                    hasAnsweredCurrentQuestion &&
                      isCorrect &&
                      "border-emerald-500 bg-emerald-500/5",
                    hasAnsweredCurrentQuestion &&
                      isSelected &&
                      !isCorrect &&
                      "border-rose-500 bg-rose-500/5",
                  )}
                  onClick={() => setSelectedAlternative(alternative.letter)}
                  disabled={hasAnsweredCurrentQuestion}
                >
                  <div className="mt-0.5">
                    {hasAnsweredCurrentQuestion && isCorrect ? (
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
              Responda e avance para a proxima questao mantendo este mesmo bloco de estudo.
            </p>
            <Button
              disabled={!selectedAlternative || hasAnsweredCurrentQuestion || isPending}
              onClick={() =>
                answerQuestion({
                  questionId,
                  chosenAlternative: selectedAlternative ?? "",
                  timeSpentSeconds: Math.max(1, Math.floor((Date.now() - startedAt) / 1000)),
                })
              }
            >
              {isPending ? "Enviando..." : "Enviar resposta"}
            </Button>
          </div>

          {activeAnswer && revealedCorrectAlternative ? (
            <AnswerFeedback
              correct={activeAnswer.correct}
              correctAlternative={revealedCorrectAlternative}
              explanation={activeAnswer.explanation}
              showMarkMasteredAction={source === "error-notebook"}
              isMarkingMastered={isUpdatingNotebookStatus}
              onMarkMastered={() =>
                updateNotebookStatus({
                  questionId,
                  payload: {
                    masteryStatus: "MASTERED",
                  },
                })
              }
            />
          ) : null}
        </CardContent>
      </Card>

      <QuestionNavigator
        currentIndex={currentIndex}
        totalCount={ids.length}
        backHref={returnTo}
        previousHref={previousHref}
        nextHref={nextHref}
        backLabel="Voltar para lista"
      />
    </div>
  );
}
