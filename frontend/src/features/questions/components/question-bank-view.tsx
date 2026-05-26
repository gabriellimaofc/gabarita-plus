"use client";

import Link from "next/link";
import { Filter, Heart, Search, SlidersHorizontal, Star } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useQuestions, useToggleFavorite } from "@/hooks/use-questions";
import { cn } from "@/lib/utils";
import type { DifficultyLevel, QuestionFilters } from "@/types/question";

const difficultyLabel: Record<DifficultyLevel, string> = {
  EASY: "Fácil",
  MEDIUM: "Média",
  HARD: "Alta",
};

export function QuestionBankView() {
  const [filters, setFilters] = useState<QuestionFilters>({
    page: 0,
    size: 10,
    direction: "DESC",
  });
  const { data, isLoading, isError, refetch } = useQuestions(filters);
  const { mutate: toggleFavorite } = useToggleFavorite();

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Banco de questões"
        title="Treine com foco, filtre melhor e responda com contexto."
        description="Use filtros por matéria, dificuldade, ano e status para transformar o banco de questões em uma ferramenta de precisão."
      />

      <Card>
        <CardContent className="grid gap-4 p-5 lg:grid-cols-[1.4fr_repeat(4,minmax(0,1fr))]">
          <div className="relative lg:col-span-2">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Buscar por título, habilidade ou conteúdo"
              className="pl-11"
              value={filters.search ?? ""}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  page: 0,
                  search: event.target.value,
                }))
              }
            />
          </div>
          <Input
            placeholder="Matéria"
            value={filters.subject ?? ""}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                page: 0,
                subject: event.target.value,
              }))
            }
          />
          <Input
            placeholder="Tópico"
            value={filters.topic ?? ""}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                page: 0,
                topic: event.target.value,
              }))
            }
          />
          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={filters.difficulty ?? ""}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                page: 0,
                difficulty: event.target.value as DifficultyLevel | "",
              }))
            }
          >
            <option value="">Dificuldade</option>
            <option value="EASY">Fácil</option>
            <option value="MEDIUM">Média</option>
            <option value="HARD">Alta</option>
          </select>
          <Button
            variant="outline"
            onClick={() =>
              setFilters((current) => ({
                ...current,
                page: 0,
                favoritesOnly: !current.favoritesOnly,
              }))
            }
          >
            <SlidersHorizontal className="size-4" />
            {filters.favoritesOnly ? "Só favoritas" : "Filtros"}
          </Button>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="grid gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-44" />
          ))}
        </div>
      ) : null}

      {isError ? (
        <ErrorState
          title="Não foi possível carregar as questões."
          description="Tente novamente ou ajuste os filtros para consultar a API outra vez."
          onRetry={() => void refetch()}
        />
      ) : null}

      {!isLoading && !isError && data && data.items.length === 0 ? (
        <EmptyState
          title="Nenhuma questão encontrada."
          description="Ajuste seus filtros ou limpe a busca para carregar mais resultados do banco."
          actionLabel="Limpar filtros"
          onAction={() =>
            setFilters({
              page: 0,
              size: 10,
              direction: "DESC",
            })
          }
        />
      ) : null}

      {!isLoading && !isError && data?.items.length ? (
        <div className="grid gap-4">
          {data.items.map((question) => (
            <Card key={question.id}>
              <CardContent className="space-y-5 p-6">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge>{question.subject}</Badge>
                      <Badge variant="secondary">{question.topic}</Badge>
                      <Badge variant="outline">
                        {difficultyLabel[question.difficulty]}
                      </Badge>
                      {question.answered ? (
                        <Badge
                          variant={
                            question.answeredCorrectly ? "success" : "warning"
                          }
                        >
                          {question.answeredCorrectly ? "Acertou" : "Revisar"}
                        </Badge>
                      ) : null}
                    </div>
                    <div>
                      <h2 className="text-xl font-semibold">{question.title}</h2>
                      <p className="mt-2 line-clamp-2 text-sm leading-7 text-muted-foreground">
                        {question.statement}
                      </p>
                    </div>
                  </div>

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => toggleFavorite(question.id)}
                  >
                    <Heart
                      className={cn(
                        "size-4",
                        question.favorite && "fill-current text-rose-500",
                      )}
                    />
                  </Button>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span className="inline-flex items-center gap-2">
                      <Filter className="size-4" />
                      {question.exam} {question.year}
                    </span>
                    <span className="inline-flex items-center gap-2">
                      <Star className="size-4" />
                      {question.competency ?? "Competência mapeada"}
                    </span>
                  </div>
                  <Link href={`/questoes/${question.id}`}>
                    <Button>Resolver questão</Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}

      {data ? (
        <div className="flex flex-col gap-4 rounded-[28px] border border-border/70 bg-background/70 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-sm text-muted-foreground">
            {data.metadata.totalElements} questões encontradas.
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              disabled={data.metadata.first}
              onClick={() =>
                setFilters((current) => ({
                  ...current,
                  page: Math.max((current.page ?? 0) - 1, 0),
                }))
              }
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              disabled={data.metadata.last}
              onClick={() =>
                setFilters((current) => ({
                  ...current,
                  page: (current.page ?? 0) + 1,
                }))
              }
            >
              Próxima
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
