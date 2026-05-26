"use client";

import Link from "next/link";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useErrorNotebook } from "@/hooks/use-questions";
import { formatDate } from "@/lib/utils";

const masteryVariant = {
  NEW: "outline",
  LEARNING: "warning",
  REVIEW: "secondary",
  MASTERED: "success",
} as const;

export function ErrorNotebookView() {
  const { data, isLoading, isError, refetch } = useErrorNotebook();

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Caderno de erros"
        title="Revise onde há mais retorno sobre esforço."
        description="Priorize tópicos frágeis, respeite a próxima revisão e mantenha constância no que mais derruba sua nota."
      />

      {isLoading ? (
        <Card>
          <CardContent className="space-y-4 p-6">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-14" />
            ))}
          </CardContent>
        </Card>
      ) : null}

      {isError ? (
        <ErrorState
          title="Não foi possível carregar o caderno de erros."
          description="Tente novamente para buscar a revisão planejada no backend."
          onRetry={() => void refetch()}
        />
      ) : null}

      {!isLoading && !isError && data?.length === 0 ? (
        <EmptyState
          title="Seu caderno de erros está limpo."
          description="Quando você errar uma questão, ela passa a aparecer aqui com prioridade de revisão."
        />
      ) : null}

      {!isLoading && !isError && data?.length ? (
        <Card>
          <CardContent className="overflow-x-auto p-0">
            <table className="min-w-full text-left">
              <thead className="border-b border-border/70 bg-muted/30">
                <tr className="text-sm text-muted-foreground">
                  <th className="px-6 py-4 font-medium">Questão</th>
                  <th className="px-6 py-4 font-medium">Matéria</th>
                  <th className="px-6 py-4 font-medium">Erros</th>
                  <th className="px-6 py-4 font-medium">Próxima revisão</th>
                  <th className="px-6 py-4 font-medium">Status</th>
                  <th className="px-6 py-4 font-medium" />
                </tr>
              </thead>
              <tbody>
                {data.map((entry) => (
                  <tr key={entry.id} className="border-b border-border/60">
                    <td className="px-6 py-5 font-medium">{entry.questionTitle}</td>
                    <td className="px-6 py-5 text-sm text-muted-foreground">
                      {entry.subject}
                    </td>
                    <td className="px-6 py-5">{entry.errorCount}</td>
                    <td className="px-6 py-5 text-sm text-muted-foreground">
                      {entry.nextReviewAt ? formatDate(entry.nextReviewAt) : "Sem agenda"}
                    </td>
                    <td className="px-6 py-5">
                      <Badge variant={masteryVariant[entry.masteryStatus]}>
                        {entry.masteryStatus}
                      </Badge>
                    </td>
                    <td className="px-6 py-5">
                      <Link href={`/questoes/${entry.questionId}`}>
                        <Button variant="outline">Revisar agora</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
