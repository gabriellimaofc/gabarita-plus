"use client";

import { cn } from "@/lib/utils";
import type { MockExamQuestion } from "@/types/mock-exam";

export function ExamAnswerCard({
  questions,
  currentQuestionId,
  onSelect,
}: {
  questions: MockExamQuestion[];
  currentQuestionId: number;
  onSelect: (questionId: number) => void;
}) {
  return (
    <div className="rounded-[28px] border border-border/70 bg-background/80 p-5">
      <div className="space-y-1">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
          Cartao-resposta
        </p>
        <p className="text-sm text-muted-foreground">
          Navegue rapidamente entre as questoes do simulado.
        </p>
      </div>

      <div className="mt-4 grid grid-cols-5 gap-2 sm:grid-cols-6 lg:grid-cols-4">
        {questions.map((question) => {
          const isCurrent = question.questionId === currentQuestionId;

          return (
            <button
              key={question.questionId}
              type="button"
              onClick={() => onSelect(question.questionId)}
              className={cn(
                "rounded-2xl border px-3 py-3 text-sm font-semibold transition",
                isCurrent
                  ? "border-primary bg-primary text-primary-foreground"
                  : question.answered
                    ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
                    : "border-border bg-background hover:bg-accent",
              )}
            >
              {question.questionOrder}
            </button>
          );
        })}
      </div>
    </div>
  );
}
