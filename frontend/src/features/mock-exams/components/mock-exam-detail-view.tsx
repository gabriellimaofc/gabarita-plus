"use client";

import { useRouter } from "next/navigation";
import { BookCheck, CheckCircle2, Circle, Clock3, Target } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { PageShellSkeleton } from "@/components/common/page-shell-skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AnswerFeedback } from "@/features/questions/components/answer-feedback";
import {
  AlternativeContent,
  QuestionContent,
} from "@/features/questions/components/question-content";
import { QuestionNavigator } from "@/features/questions/components/question-navigator";
import { QuestionProgress } from "@/features/questions/components/question-progress";
import { ExamAnswerCard } from "@/features/mock-exams/components/exam-answer-card";
import { ExamTimer } from "@/features/mock-exams/components/exam-timer";
import {
  useFinishMockExam,
  useMockExam,
  useMockExamQuestions,
  useMockExamResult,
  useSaveMockExamAnswer,
} from "@/hooks/use-mock-exams";
import { cn, formatDate, formatSecondsToMinutes } from "@/lib/utils";

export function MockExamDetailView({ mockExamId }: { mockExamId: number }) {
  const router = useRouter();
  const { data: exam, isLoading: isLoadingExam, isError: isExamError, refetch: refetchExam } =
    useMockExam(mockExamId);
  const { data: questions, isLoading: isLoadingQuestions, isError: isQuestionsError, refetch: refetchQuestions } =
    useMockExamQuestions(mockExamId, Boolean(exam && !exam.finished));
  const { data: result, isLoading: isLoadingResult, isError: isResultError, refetch: refetchResult } =
    useMockExamResult(mockExamId, Boolean(exam?.finished));
  const saveAnswer = useSaveMockExamAnswer();
  const finishMockExam = useFinishMockExam();
  const [currentIndex, setCurrentIndex] = useState(0);
  const [draftSelections, setDraftSelections] = useState<Record<number, string>>({});
  const [questionStartedAt, setQuestionStartedAt] = useState(Date.now());
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [autoFinished, setAutoFinished] = useState(false);

  useEffect(() => {
    if (questions?.length) {
      setCurrentIndex((current) => Math.min(current, questions.length - 1));
    }
  }, [questions]);

  const currentQuestion = questions?.[currentIndex];

  useEffect(() => {
    setQuestionStartedAt(Date.now());
  }, [currentQuestion?.questionId]);

  const answerCardQuestions = exam?.questions ?? [];

  const currentSelection = currentQuestion
    ? draftSelections[currentQuestion.questionId] ?? currentQuestion.chosenAlternative ?? null
    : null;

  const handleFinish = () => {
    if (finishMockExam.isPending) {
      return;
    }

    const confirmed = window.confirm(
      "Deseja finalizar o simulado agora? As respostas salvas serao corrigidas e o resultado sera gerado imediatamente.",
    );

    if (!confirmed) {
      return;
    }

    finishMockExam.mutate({
      mockExamId,
      payload: {
        timeSpentSeconds: elapsedSeconds,
      },
    });
  };

  const headerContent = useMemo(() => {
    if (!exam) {
      return null;
    }

    return (
      <PageHeader
        eyebrow="Simulado"
        title={exam.title}
        description={
          exam.finished
            ? "Resultado consolidado com nota final, desempenho por matéria e revisão detalhada das questões."
            : "Resolva uma questão por vez, com cronômetro, cartão-resposta e salvamento automatico."
        }
      />
    );
  }, [exam]);

  if (isLoadingExam) {
    return <PageShellSkeleton />;
  }

  if (isExamError || !exam) {
    return (
      <ErrorState
        title="Não foi possível carregar este simulado."
        description="Tente novamente para sincronizar o fluxo completo com o backend."
        onRetry={() => void refetchExam()}
      />
    );
  }

  if (exam.finished) {
    if (isLoadingResult) {
      return <PageShellSkeleton />;
    }

    if (isResultError || !result) {
      return (
        <ErrorState
          title="Não foi possível carregar o resultado deste simulado."
          description="Tente novamente para recuperar a correção e o desempenho por matéria."
          onRetry={() => void refetchResult()}
        />
      );
    }

    return (
      <div className="space-y-8">
        {headerContent}

        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="space-y-2 p-5">
              <p className="text-sm text-muted-foreground">Nota final</p>
              <p className="text-3xl font-semibold">{result.finalScore.toFixed(1)}</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-2 p-5">
              <p className="text-sm text-muted-foreground">Acertos</p>
              <p className="text-3xl font-semibold">{result.correctAnswers}</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-2 p-5">
              <p className="text-sm text-muted-foreground">Erros</p>
              <p className="text-3xl font-semibold">{result.incorrectAnswers}</p>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-2 p-5">
              <p className="text-sm text-muted-foreground">Tempo total</p>
              <p className="text-2xl font-semibold">
                {result.timeSpentSeconds
                  ? formatSecondsToMinutes(result.timeSpentSeconds)
                  : "--"}
              </p>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Desempenho por matéria</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {result.performanceBySubject.map((subject) => (
              <div
                key={subject.subject}
                className="rounded-[24px] border border-border/70 bg-background/70 p-5"
              >
                <div className="flex items-center justify-between gap-3">
                  <p className="font-semibold">{subject.subject}</p>
                  <Badge variant="outline">{subject.accuracy.toFixed(1)}%</Badge>
                </div>
                <div className="mt-4 grid gap-2 text-sm text-muted-foreground">
                  <p>{subject.totalQuestions} questões</p>
                  <p>{subject.correctAnswers} acerto(s)</p>
                  <p>{subject.incorrectAnswers} erro(s)</p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        <div className="space-y-4">
          <h2 className="text-2xl font-semibold">Revisão detalhada</h2>

          {result.questions.map((question) => (
            <Card key={question.questionId}>
              <CardContent className="space-y-5 p-6">
                <div className="space-y-3">
                  <div className="flex flex-wrap gap-2">
                    <Badge>{question.subject}</Badge>
                    <Badge variant="secondary">{question.topic}</Badge>
                    <Badge variant={question.correct ? "success" : "warning"}>
                      {question.correct ? "Acertou" : "Errou"}
                    </Badge>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold">
                      {question.questionOrder}. {question.title}
                    </h3>
                    <div className="mt-2">
                      <QuestionContent
                        statement={question.statement}
                        statementHtml={question.statementHtml}
                        assets={question.assets}
                      />
                    </div>
                  </div>
                </div>

                <div className="grid gap-3">
                  {question.alternatives.map((alternative) => {
                    const isChosen = question.chosenAlternative === alternative.letter;
                    const isCorrect = question.correctAlternative === alternative.letter;

                    return (
                      <div
                        key={alternative.id}
                        className={cn(
                          "rounded-[22px] border px-4 py-3",
                          isCorrect
                            ? "border-emerald-500/40 bg-emerald-500/5"
                            : isChosen
                              ? "border-rose-500/40 bg-rose-500/5"
                              : "border-border bg-background/60",
                        )}
                      >
                        <p className="font-semibold">{alternative.letter}</p>
                        <div className="mt-1">
                          <AlternativeContent alternative={alternative} />
                        </div>
                      </div>
                    );
                  })}
                </div>

                {question.correctAlternative ? (
                  <AnswerFeedback
                    correct={Boolean(question.correct)}
                    correctAlternative={question.correctAlternative}
                    explanation={question.explanation}
                  />
                ) : null}
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (isLoadingQuestions) {
    return <PageShellSkeleton />;
  }

  if (isQuestionsError) {
    return (
      <ErrorState
        title="Não foi possível carregar as questões deste simulado."
        description="Tente novamente para retomar a prova com seguranca."
        onRetry={() => void refetchQuestions()}
      />
    );
  }

  if (!questions?.length || !currentQuestion) {
    return (
      <EmptyState
        title="Nenhuma questão encontrada neste simulado."
        description="Crie um novo simulado ou volte para a lista para verificar a configuracao."
        actionLabel="Voltar para simulados"
        onAction={() => router.push("/simulados")}
      />
    );
  }

  return (
    <div className="space-y-6">
      {headerContent}

      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <QuestionProgress
          current={currentIndex + 1}
          total={questions.length}
          answeredCount={exam.answeredCount}
          label="Progresso da prova"
        />
        <ExamTimer
          examId={exam.id}
          durationMinutes={exam.durationMinutes}
          finished={exam.finished}
          onElapsedChange={setElapsedSeconds}
          onExpire={() => {
            if (autoFinished || finishMockExam.isPending) {
              return;
            }

            setAutoFinished(true);
            finishMockExam.mutate({
              mockExamId,
              payload: {
                timeSpentSeconds: exam.durationMinutes * 60,
              },
            });
          }}
        />
      </div>

      <QuestionNavigator
        currentIndex={currentIndex}
        totalCount={questions.length}
        backHref="/simulados"
        onPrevious={() => setCurrentIndex((current) => Math.max(current - 1, 0))}
        onNext={() =>
          setCurrentIndex((current) => Math.min(current + 1, questions.length - 1))
        }
        previousDisabled={currentIndex === 0}
        nextDisabled={currentIndex >= questions.length - 1}
        backLabel="Voltar para simulados"
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Card>
            <CardHeader className="space-y-4">
              <div className="flex flex-wrap gap-2">
                <Badge>{currentQuestion.subject}</Badge>
                <Badge variant="secondary">{currentQuestion.topic}</Badge>
                <Badge variant="outline">{currentQuestion.difficulty}</Badge>
              </div>
              <CardTitle className="text-2xl">
                {currentQuestion.questionOrder}. {currentQuestion.title}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-8">
              <QuestionContent
                statement={currentQuestion.statement}
                statementHtml={currentQuestion.statementHtml}
                assets={currentQuestion.assets}
                sourceLabel="Recursos oficiais da questão"
              />

              <div className="space-y-3">
                {currentQuestion.alternatives.map((alternative) => {
                  const isSelected = currentSelection === alternative.letter;

                  return (
                    <button
                      key={alternative.id}
                      type="button"
                      className={cn(
                        "flex w-full items-start gap-4 rounded-[24px] border px-5 py-4 text-left transition",
                        isSelected
                          ? "border-primary bg-primary/5"
                          : "border-border bg-background/60 hover:bg-accent",
                        saveAnswer.isPending &&
                          isSelected &&
                          "opacity-80",
                      )}
                      onClick={() => {
                        setDraftSelections((current) => ({
                          ...current,
                          [currentQuestion.questionId]: alternative.letter,
                        }));

                        saveAnswer.mutate({
                          mockExamId,
                          payload: {
                            questionId: currentQuestion.questionId,
                            chosenAlternative: alternative.letter,
                            timeSpentSeconds: Math.max(
                              1,
                              Math.floor((Date.now() - questionStartedAt) / 1000),
                            ),
                          },
                        });
                      }}
                      disabled={finishMockExam.isPending}
                    >
                      <div className="mt-0.5">
                        {isSelected ? (
                          <CheckCircle2 className="size-5 text-primary" />
                        ) : (
                          <Circle className="size-5" />
                        )}
                      </div>
                      <div>
                        <p className="font-semibold">{alternative.letter}</p>
                        <div className="mt-1">
                          <AlternativeContent alternative={alternative} />
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>

              <div className="rounded-[24px] border border-border/70 bg-muted/20 p-4 text-sm text-muted-foreground">
                As respostas são salvas automaticamente ao selecionar uma alternativa. Você pode voltar e trocar a resposta antes de finalizar o simulado.
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <ExamAnswerCard
            questions={answerCardQuestions}
            currentQuestionId={currentQuestion.questionId}
            onSelect={(questionId) => {
              const nextIndex = answerCardQuestions.findIndex(
                (question) => question.questionId === questionId,
              );

              if (nextIndex >= 0) {
                setCurrentIndex(nextIndex);
              }
            }}
          />

          <Card>
            <CardContent className="space-y-4 p-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
                  Resumo da prova
                </p>
                <p className="mt-1 text-lg font-semibold">{exam.title}</p>
              </div>

              <div className="grid gap-3 text-sm text-muted-foreground">
                <div className="flex items-center gap-3">
                  <Target className="size-4 text-primary" />
                  <span>{exam.answeredCount} questões respondidas</span>
                </div>
                <div className="flex items-center gap-3">
                  <Clock3 className="size-4 text-primary" />
                  <span>{exam.durationMinutes} minutos de prova</span>
                </div>
                <div className="flex items-center gap-3">
                  <BookCheck className="size-4 text-primary" />
                  <span>Criado em {formatDate(exam.createdAt)}</span>
                </div>
              </div>

              <Button
                className="w-full"
                onClick={handleFinish}
                disabled={finishMockExam.isPending}
              >
                {finishMockExam.isPending ? "Finalizando..." : "Finalizar simulado"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
