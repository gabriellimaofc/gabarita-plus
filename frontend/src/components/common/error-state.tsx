import { AlertTriangle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

export function ErrorState({
  title = "Não foi possível carregar este conteúdo.",
  description = "Tente novamente em alguns instantes.",
  actionLabel = "Tentar novamente",
  onRetry,
}: {
  title?: string;
  description?: string;
  actionLabel?: string;
  onRetry?: () => void;
}) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-4 p-8 text-center">
        <div className="flex size-14 items-center justify-center rounded-full bg-amber-500/10 text-amber-500">
          <AlertTriangle className="size-6" />
        </div>
        <div className="space-y-2">
          <h2 className="text-xl font-semibold">{title}</h2>
          <p className="max-w-lg text-sm leading-7 text-muted-foreground">{description}</p>
        </div>
        {onRetry ? (
          <Button type="button" variant="outline" onClick={onRetry}>
            {actionLabel}
          </Button>
        ) : null}
      </CardContent>
    </Card>
  );
}
