"use client";

import { Progress } from "@/components/ui/progress";

export function QuestionProgress({
  current,
  total,
  answeredCount,
  label = "Progresso",
}: {
  current: number;
  total: number;
  answeredCount?: number;
  label?: string;
}) {
  const percentage = total > 0 ? (current / total) * 100 : 0;

  return (
    <div className="space-y-3 rounded-[28px] border border-border/70 bg-background/80 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
            {label}
          </p>
          <p className="mt-1 text-lg font-semibold">
            Questao {Math.max(current, 1)} de {Math.max(total, 1)}
          </p>
        </div>
        <div className="text-right text-sm text-muted-foreground">
          <p>{Math.round(percentage)}% do bloco percorrido</p>
          {answeredCount !== undefined ? (
            <p>{answeredCount} resposta(s) registradas</p>
          ) : null}
        </div>
      </div>
      <Progress value={percentage} className="h-2.5" />
    </div>
  );
}
