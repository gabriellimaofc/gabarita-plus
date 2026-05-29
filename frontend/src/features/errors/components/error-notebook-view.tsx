"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { BookOpenCheck, Flame, Layers3, RefreshCw } from "lucide-react";
import { startTransition } from "react";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  buildErrorNotebookParams,
  parseErrorNotebookFilters,
} from "@/features/errors/lib/error-notebook-filters";
import { useErrorNotebook } from "@/hooks/use-questions";
import { cn, formatDate } from "@/lib/utils";
import type {
  DifficultyLevel,
  ErrorNotebookEntry,
  MasteryStatus,
  ReviewPriority,
} from "@/types/question";

const masteryLabel: Record<MasteryStatus, string> = {
  NEW: "Pendente",
  LEARNING: "Pendente",
  REVIEW: "Em revisão",
  MASTERED: "Dominada",
};

const masteryVariant = {
  NEW: "outline",
  LEARNING: "warning",
  REVIEW: "secondary",
  MASTERED: "success",
} as const;

const priorityLabel: Record<ReviewPriority, string> = {
  HIGH: "Alta",
  MEDIUM: "Média",
  LOW: "Baixa",
};

const priorityTone: Record<ReviewPriority, string> = {
  HIGH: "text-rose-500",
  MEDIUM: "text-amber-500",
  LOW: "text-emerald-500",
};

function createQuestionReviewHref({
  entries,
  index,
  returnTo,
}: {
  entries: ErrorNotebookEntry[];
  index: number;
  returnTo: string;
}) {
  const params = new URLSearchParams({
    ids: entries.map((entry) => entry.questionId).join(","),
    index: String(index),
    returnTo,
    source: "error-notebook",
  });

  return `/questoes/${entries[index]?.questionId}?${params.toString()}`;
}

export function ErrorNotebookView() {
  const router = useRouter();
  const pathname = usePathname();
  const rawSearchParams = useSearchParams();
  const searchParams = new URLSearchParams(rawSearchParams.toString());
  const filters = parseErrorNotebookFilters(searchParams);
  const { data, isLoading, isError, refetch } = useErrorNotebook(filters);

  const updateFilters = (updates: Partial<typeof filters>) => {
    const nextFilters = {
      ...filters,
      ...updates,
    };
    const params = buildErrorNotebookParams(nextFilters);

    startTransition(() => {
      router.replace(params.toString() ? `${pathname}?${params.toString()}` : pathname, {
        scroll: false,
      });
    });
  };

  const clearFilters = () => {
    startTransition(() => {
      router.replace(pathname, { scroll: false });
    });
  };

  const returnTo = rawSearchParams.toString()
    ? `/caderno-erros?${rawSearchParams.toString()}`
    : "/caderno-erros";

  const highPriorityCount = data?.filter((entry) => entry.priority === "HIGH").length ?? 0;
  const reviewCount = data?.filter((entry) => entry.masteryStatus === "REVIEW").length ?? 0;
  const masteredCount = data?.filter((entry) => entry.masteryStatus === "MASTERED").length ?? 0;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Caderno de erros"
        title="Transforme erros em uma trilha de revisão de verdade."
        description="Filtre por matéria, tópico, dificuldade, status e prioridade. Entre em modo revisão e percorra as questões erradas em sequência, com contexto de estudo real."
      />

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-4 p-5">
            <div className="rounded-2xl bg-rose-500/10 p-3 text-rose-500">
              <Flame className="size-5" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Prioridade alta</p>
              <p className="text-2xl font-semibold">{highPriorityCount}</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center gap-4 p-5">
            <div className="rounded-2xl bg-amber-500/10 p-3 text-amber-500">
              <RefreshCw className="size-5" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Em revisão</p>
              <p className="text-2xl font-semibold">{reviewCount}</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center gap-4 p-5">
            <div className="rounded-2xl bg-emerald-500/10 p-3 text-emerald-500">
              <BookOpenCheck className="size-5" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Dominadas</p>
              <p className="text-2xl font-semibold">{masteredCount}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-5">
          <Input
            placeholder="Matéria"
            value={filters.subject ?? ""}
            onChange={(event) => updateFilters({ subject: event.target.value })}
          />

          <Input
            placeholder="Tópico"
            value={filters.topic ?? ""}
            onChange={(event) => updateFilters({ topic: event.target.value })}
          />

          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={filters.difficulty ?? ""}
            onChange={(event) =>
              updateFilters({
                difficulty: event.target.value as DifficultyLevel | "",
              })
            }
          >
            <option value="">Dificuldade</option>
            <option value="EASY">Fácil</option>
            <option value="MEDIUM">Média</option>
            <option value="HARD">Alta</option>
          </select>

          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={filters.masteryStatus ?? ""}
            onChange={(event) =>
              updateFilters({
                masteryStatus: event.target.value as MasteryStatus | "",
              })
            }
          >
            <option value="">Status</option>
            <option value="NEW">Pendente</option>
            <option value="LEARNING">Aprendendo</option>
            <option value="REVIEW">Em revisão</option>
            <option value="MASTERED">Dominada</option>
          </select>

          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={filters.priority ?? ""}
            onChange={(event) =>
              updateFilters({
                priority: event.target.value as ReviewPriority | "",
              })
            }
          >
            <option value="">Prioridade</option>
            <option value="HIGH">Alta</option>
            <option value="MEDIUM">Média</option>
            <option value="LOW">Baixa</option>
          </select>

          <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-5">
            <Link
              href={
                rawSearchParams.toString()
                  ? `/caderno-erros/revisão?${rawSearchParams.toString()}`
                  : "/caderno-erros/revisão"
              }
            >
              <Button>
                <Layers3 className="size-4" />
                Modo revisão
              </Button>
            </Link>

            <Button variant="outline" onClick={clearFilters}>
              Limpar filtros
            </Button>
          </div>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-56" />
          ))}
        </div>
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
          title="Seu caderno de erros esta limpo."
          description="Quando você errar uma questão, ela passa a aparecer aqui com prioridade de revisão."
        />
      ) : null}

      {!isLoading && !isError && data?.length ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {data.map((entry, index) => (
            <Card key={entry.id}>
              <CardContent className="space-y-5 p-6">
                <div className="space-y-3">
                  <div className="flex flex-wrap gap-2">
                    <Badge>{entry.subject}</Badge>
                    <Badge variant="secondary">{entry.topic}</Badge>
                    <Badge variant={masteryVariant[entry.masteryStatus]}>
                      {masteryLabel[entry.masteryStatus]}
                    </Badge>
                    <Badge variant="outline" className={cn(priorityTone[entry.priority])}>
                      Prioridade {priorityLabel[entry.priority]}
                    </Badge>
                  </div>
                  <div>
                    <h2 className="text-lg font-semibold">{entry.questionTitle}</h2>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {entry.errorCount} erro(s) acumulados nesta questão.
                    </p>
                  </div>
                </div>

                <div className="grid gap-3 text-sm text-muted-foreground sm:grid-cols-2">
                  <div>
                    <p className="font-medium text-foreground">Ultimo erro</p>
                    <p>{entry.lastErrorAt ? formatDate(entry.lastErrorAt) : "Sem registro"}</p>
                  </div>
                  <div>
                    <p className="font-medium text-foreground">Última revisão</p>
                    <p>
                      {entry.lastReviewedAt
                        ? formatDate(entry.lastReviewedAt)
                        : "Ainda não revisada"}
                    </p>
                  </div>
                  <div>
                    <p className="font-medium text-foreground">Próxima revisão</p>
                    <p>
                      {entry.nextReviewAt ? formatDate(entry.nextReviewAt) : "Sem agenda"}
                    </p>
                  </div>
                  <div>
                    <p className="font-medium text-foreground">Dificuldade</p>
                    <p>{entry.difficulty}</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  <Link
                    href={createQuestionReviewHref({
                      entries: data,
                      index,
                      returnTo,
                    })}
                  >
                    <Button>Revisar agora</Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}
    </div>
  );
}
