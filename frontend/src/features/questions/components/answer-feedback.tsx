"use client";

import { CheckCircle2, CircleAlert } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function AnswerFeedback({
  correct,
  correctAlternative,
  explanation,
  onMarkMastered,
  isMarkingMastered,
  showMarkMasteredAction,
}: {
  correct: boolean;
  correctAlternative: string;
  explanation?: string | null;
  onMarkMastered?: () => void;
  isMarkingMastered?: boolean;
  showMarkMasteredAction?: boolean;
}) {
  return (
    <div
      className={cn(
        "rounded-[28px] border p-5",
        correct ? "border-emerald-500/30 bg-emerald-500/5" : "border-amber-500/30 bg-amber-500/5",
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            {correct ? (
              <CheckCircle2 className="size-5 text-emerald-500" />
            ) : (
              <CircleAlert className="size-5 text-amber-500" />
            )}
            <p className="font-semibold">
              {correct ? "Voce acertou." : "Voce errou, mas da para transformar isso em revisao util."}
            </p>
          </div>
          <p className="text-sm leading-7 text-muted-foreground">
            Alternativa correta: <strong className="text-foreground">{correctAlternative}</strong>
          </p>
        </div>

        {showMarkMasteredAction && correct && onMarkMastered ? (
          <Button
            type="button"
            variant="outline"
            onClick={onMarkMastered}
            disabled={isMarkingMastered}
          >
            {isMarkingMastered ? "Salvando..." : "Marcar como dominada"}
          </Button>
        ) : null}
      </div>

      <div className="mt-4 rounded-2xl bg-background/75 p-4 text-sm leading-7 text-muted-foreground">
        {explanation ?? "A explicacao detalhada ainda nao foi informada para esta questao."}
      </div>
    </div>
  );
}
