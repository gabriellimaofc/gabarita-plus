"use client";

import Link from "next/link";
import { ArrowLeft, CheckCircle2, Circle, Heart, Timer } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { ErrorState } from "@/components/common/error-state";
import { PageShellSkeleton } from "@/components/common/page-shell-skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAnswerQuestion, useQuestion, useToggleFavorite } from "@/hooks/use-questions";
import { cn } from "@/lib/utils";

export function QuestionDetailView({ questionId }: { questionId: number }) {
  const { data: question, isLoading, isError, refetch } = useQuestion(questionId);
  const { mutateAsync: answerQuestion, data: answerResult, isPending } = useAnswerQuestion();
  const { mutate: toggleFavorite } = useToggleFavorite();
  const [selectedAlternative, setSelectedAlternative] = useState<string | null>(null);
  const [startedAt] = useState(Date.now());

  useEffect(() => {
    setSelectedAlternative(null);
  }, [questionId]);

  const feedback = useMemo(() => {
    if (!answerResult || !question) {
      return null;
    }

    return {
      correct: answerResult.correct,
      message: answerResult.correct
        ? "Você leu bem o enunciado e escolheu a alternativa correta."
        : `A correta era ${question.correctAlternative}. Revise a explicação abaixo antes de seguir.`,
    };
  }, [answerResult, question]);

  if (isLoading) {
    return <PageShellSkeleton />;
  }

  if (isError || !question) {
    return (
      <ErrorState
        title="Não foi possível carregar esta questão."
        description="Tente novamente para buscar os dados atualizados do backend."
        onRetry={() => void refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href="/questoes">
          <Button variant="outline">
            <ArrowLeft className="size-4" />
            Voltar ao banco
          </Button>
        </Link>
        <Button variant="outline" onClick={() => toggleFavorite(question.id)}>
          <Heart className={cn("size-4", question.favorite && "fill-current text-rose-500")} />
          Favoritar
        </Button>
      </div>

      <Card>
        <CardHeader>
          <div className="flex flex-wrap gap-2">
            <Badge>{question.subject}</Badge>
            <Badge variant="secondary">{question.topic}</Badge>
            <Badge variant="outline">
              <Timer className="mr-1 size-3.5" />
              Ritmo monitorado
            </Badge>
          </div>
          <CardTitle className="text-2xl">{question.title}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-8">
          <p className="text-sm leading-8 text-muted-foreground">{question.statement}</p>

          <div className="space-y-3">
            {question.alternatives.map((alternative) => {
              const isSelected = selectedAlternative === alternative.letter;
              const hasAnswered = Boolean(answerResult);
              const isCorrect = alternative.letter === question.correctAlternative;

              return (
                <button
                  key={alternative.id}
                  type="button"
                  className={cn(
                    "flex w-full items-start gap-4 rounded-[24px] border px-5 py-4 text-left transition",
                    isSelected
                      ? "border-primary bg-primary/5"
                      : "border-border bg-background/60 hover:bg-accent",
                    hasAnswered && isCorrect && "border-emerald-500 bg-emerald-500/5",
                    hasAnswered &&
                      isSelected &&
                      !isCorrect &&
                      "border-rose-500 bg-rose-500/5",
                  )}
                  onClick={() => setSelectedAlternative(alternative.letter)}
                  disabled={Boolean(answerResult)}
                >
                  <div className="mt-0.5">
                    {hasAnswered && isCorrect ? (
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
              Tempo de resposta enviado para análise de ritmo e precisão.
            </p>
            <Button
              disabled={!selectedAlternative || Boolean(answerResult) || isPending}
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

          {feedback ? (
            <div
              className={cn(
                "rounded-[28px] border p-5",
                feedback.correct
                  ? "border-emerald-500/30 bg-emerald-500/5"
                  : "border-amber-500/30 bg-amber-500/5",
              )}
            >
              <p className="font-semibold">
                {feedback.correct ? "Mandou bem." : "Hora de consolidar."}
              </p>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                {feedback.message}
              </p>
              <div className="mt-4 rounded-2xl bg-background/70 p-4 text-sm leading-7 text-muted-foreground">
                {question.explanation ?? "Explicação detalhada não informada pela API."}
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
