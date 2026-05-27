"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, Filter, Heart, Search, SlidersHorizontal, Star } from "lucide-react";
import { startTransition } from "react";

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
  EASY: "Facil",
  MEDIUM: "Media",
  HARD: "Alta",
};

function parseBooleanParam(value: string | null) {
  if (value === "true") {
    return true;
  }

  if (value === "false") {
    return false;
  }

  return "";
}

function parseFilters(searchParams: URLSearchParams): QuestionFilters {
  const page = Number(searchParams.get("page") ?? "0");
  const size = Number(searchParams.get("size") ?? "10");
  const year = searchParams.get("year");

  return {
    page: Number.isFinite(page) ? page : 0,
    size: Number.isFinite(size) ? size : 10,
    direction: (searchParams.get("direction") as "ASC" | "DESC") ?? "DESC",
    sortBy: searchParams.get("sortBy") ?? "createdAt",
    search: searchParams.get("search") ?? "",
    subject: searchParams.get("subject") ?? "",
    topic: searchParams.get("topic") ?? "",
    difficulty: (searchParams.get("difficulty") as DifficultyLevel | null) ?? "",
    answered: parseBooleanParam(searchParams.get("answered")),
    incorrectOnly: searchParams.get("incorrectOnly") === "true",
    favoritesOnly: searchParams.get("favoritesOnly") === "true",
    year: year ? Number(year) : "",
  };
}

export function QuestionBankView() {
  const router = useRouter();
  const pathname = usePathname();
  const rawSearchParams = useSearchParams();
  const searchParams = new URLSearchParams(rawSearchParams.toString());
  const filters = parseFilters(searchParams);
  const { data, isLoading, isError, refetch } = useQuestions(filters);
  const { mutate: toggleFavorite } = useToggleFavorite();

  const updateFilters = (updates: Partial<QuestionFilters>) => {
    const nextParams = new URLSearchParams(rawSearchParams.toString());

    Object.entries(updates).forEach(([key, value]) => {
      if (
        value === undefined ||
        value === null ||
        value === "" ||
        ((key === "incorrectOnly" || key === "favoritesOnly") && value === false) ||
        (key === "page" && value === 0) ||
        (key === "size" && value === 10) ||
        (key === "direction" && value === "DESC") ||
        (key === "sortBy" && value === "createdAt")
      ) {
        nextParams.delete(key);
        return;
      }

      nextParams.set(key, String(value));
    });

    if (!updates.page) {
      nextParams.set("page", "0");
    }

    startTransition(() => {
      router.replace(
        nextParams.toString() ? `${pathname}?${nextParams.toString()}` : pathname,
        { scroll: false },
      );
    });
  };

  const clearFilters = () => {
    startTransition(() => {
      router.replace(pathname, { scroll: false });
    });
  };

  const returnTo = rawSearchParams.toString()
    ? `/questoes?${rawSearchParams.toString()}`
    : "/questoes";
  const currentIds = data?.items.map((question) => question.id).join(",") ?? "";

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Banco de questoes"
        title="Treine em sequencia, com filtros mais uteis e sem perder o contexto."
        description="Filtre por materia, topico, dificuldade e status. Depois, resolva varias questoes em seguida com navegacao fluida entre elas."
      />

      <Card>
        <CardContent className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-6">
          <div className="relative md:col-span-2 xl:col-span-2">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Buscar por titulo, habilidade ou conteudo"
              className="pl-11"
              value={filters.search ?? ""}
              onChange={(event) =>
                updateFilters({
                  page: 0,
                  search: event.target.value,
                })
              }
            />
          </div>

          <Input
            placeholder="Materia"
            value={filters.subject ?? ""}
            onChange={(event) =>
              updateFilters({
                page: 0,
                subject: event.target.value,
              })
            }
          />

          <Input
            placeholder="Topico"
            value={filters.topic ?? ""}
            onChange={(event) =>
              updateFilters({
                page: 0,
                topic: event.target.value,
              })
            }
          />

          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={filters.difficulty ?? ""}
            onChange={(event) =>
              updateFilters({
                page: 0,
                difficulty: event.target.value as DifficultyLevel | "",
              })
            }
          >
            <option value="">Dificuldade</option>
            <option value="EASY">Facil</option>
            <option value="MEDIUM">Media</option>
            <option value="HARD">Alta</option>
          </select>

          <select
            className="h-11 rounded-2xl border border-border bg-background/70 px-4 text-sm"
            value={
              filters.answered === ""
                ? ""
                : filters.answered
                  ? "answered"
                  : "unanswered"
            }
            onChange={(event) =>
              updateFilters({
                page: 0,
                answered:
                  event.target.value === ""
                    ? ""
                    : event.target.value === "answered",
              })
            }
          >
            <option value="">Status</option>
            <option value="answered">Respondidas</option>
            <option value="unanswered">Nao respondidas</option>
          </select>

          <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-6">
            <Button
              variant={filters.favoritesOnly ? "default" : "outline"}
              onClick={() =>
                updateFilters({
                  page: 0,
                  favoritesOnly: !filters.favoritesOnly,
                })
              }
            >
              <Heart className="size-4" />
              {filters.favoritesOnly ? "Favoritas" : "So favoritas"}
            </Button>

            <Button
              variant={filters.incorrectOnly ? "default" : "outline"}
              onClick={() =>
                updateFilters({
                  page: 0,
                  incorrectOnly: !filters.incorrectOnly,
                })
              }
            >
              <SlidersHorizontal className="size-4" />
              {filters.incorrectOnly ? "So erros" : "Revisar erros"}
            </Button>

            <Button variant="outline" onClick={clearFilters}>
              <Filter className="size-4" />
              Limpar filtros
            </Button>
          </div>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="grid gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-52" />
          ))}
        </div>
      ) : null}

      {isError ? (
        <ErrorState
          title="Nao foi possivel carregar as questoes."
          description="Tente novamente ou ajuste os filtros para consultar a API outra vez."
          onRetry={() => void refetch()}
        />
      ) : null}

      {!isLoading && !isError && data && data.items.length === 0 ? (
        <EmptyState
          title="Nenhuma questao encontrada."
          description="Ajuste seus filtros ou limpe a busca para carregar mais resultados do banco."
          actionLabel="Limpar filtros"
          onAction={clearFilters}
        />
      ) : null}

      {!isLoading && !isError && data?.items.length ? (
        <div className="grid gap-4">
          {data.items.map((question, index) => {
            const linkSearchParams = new URLSearchParams({
              ids: currentIds,
              index: String(index),
              returnTo,
            });

            return (
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
                            {question.answeredCorrectly ? "Acertou" : "Errou"}
                          </Badge>
                        ) : (
                          <Badge variant="secondary">Nova</Badge>
                        )}
                      </div>

                      <div>
                        <h2 className="text-xl font-semibold">{question.title}</h2>
                        <p className="mt-2 line-clamp-3 text-sm leading-7 text-muted-foreground">
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

                  <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
                    <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                      <span className="inline-flex items-center gap-2">
                        <Filter className="size-4" />
                        {question.exam} {question.year}
                      </span>
                      <span className="inline-flex items-center gap-2">
                        <Star className="size-4" />
                        {question.competency ?? "Competencia mapeada"}
                      </span>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      <Link href={`/questoes/${question.id}?${linkSearchParams.toString()}`}>
                        <Button>
                          Resolver em sequencia
                          <ArrowRight className="size-4" />
                        </Button>
                      </Link>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : null}

      {data ? (
        <div className="flex flex-col gap-4 rounded-[28px] border border-border/70 bg-background/70 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-sm text-muted-foreground">
            {data.metadata.totalElements} questoes encontradas.
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              disabled={data.metadata.first}
              onClick={() =>
                updateFilters({
                  page: Math.max((filters.page ?? 0) - 1, 0),
                })
              }
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              disabled={data.metadata.last}
              onClick={() =>
                updateFilters({
                  page: (filters.page ?? 0) + 1,
                })
              }
            >
              Proxima
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
