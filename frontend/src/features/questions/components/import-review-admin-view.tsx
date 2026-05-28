"use client";

import Link from "next/link";
import { AlertTriangle, CheckCircle2, ExternalLink, FileSearch, ShieldAlert } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  usePublishReviewQuestion,
  useReviewQuestion,
  useReviewQuestions,
  useUpdateReviewStatus,
  useValidateOfficialSource,
} from "@/hooks/use-admin-import-review";
import { useProtectedRoute } from "@/hooks/use-protected-route";
import { useAuthStore } from "@/store/auth-store";
import type {
  QuestionImportStatus,
  ReviewOfficialValidationPayload,
  ReviewQuestionDetail,
  ReviewQuestionFilters,
  ReviewQuestionSummary,
} from "@/types/question";
import { AlternativeContent, QuestionContent } from "@/features/questions/components/question-content";
import { cn } from "@/lib/utils";

const statusOptions: Array<{ value: QuestionImportStatus | ""; label: string }> = [
  { value: "", label: "Todos os status em revisao" },
  { value: "NEEDS_REVIEW", label: "Needs review" },
  { value: "DRAFT", label: "Draft" },
  { value: "VALIDATED", label: "Validated" },
  { value: "INVALID", label: "Invalid" },
  { value: "PUBLISHED", label: "Published" },
];

const visualKeywords = [
  "grafico",
  "gráfico",
  "figura",
  "imagem",
  "mapa",
  "tabela",
  "charge",
  "tirinha",
  "esquema",
  "desenho",
  "ilustracao",
  "ilustração",
  "diagrama",
  "observe",
  "conforme mostrado",
  "a seguir",
];

const mojibakeMarkers = ["Ã", "â€œ", "â€", "â€“", "ТЕХТО", "текст"];

function statusBadgeVariant(status: QuestionImportStatus) {
  switch (status) {
    case "PUBLISHED":
      return "success";
    case "VALIDATED":
      return "secondary";
    case "INVALID":
      return "danger";
    case "NEEDS_REVIEW":
      return "warning";
    default:
      return "outline";
  }
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function markdownToHtml(markdown: string | null) {
  if (!markdown) {
    return null;
  }

  const normalized = markdown.trim();
  if (!normalized) {
    return null;
  }

  const imageTokenPrefix = "__GP_IMAGE__";
  let imageIndex = 0;
  const imageMap = new Map<string, string>();

  const withTokens = normalized.replace(/!\[(.*?)\]\((.*?)\)/g, (_, alt, src) => {
    const token = `${imageTokenPrefix}${imageIndex++}__`;
    const safeAlt = escapeHtml(String(alt ?? "").trim() || "Imagem referenciada no enunciado");
    const safeSrc = escapeHtml(String(src ?? "").trim());
    imageMap.set(
      token,
      `<figure class="space-y-2 rounded-[24px] border border-border/70 bg-background/70 p-3"><img src="${safeSrc}" alt="${safeAlt}" class="max-h-[420px] w-full object-contain rounded-[18px] bg-muted/20" /><figcaption class="text-xs text-muted-foreground">${safeAlt}</figcaption></figure>`,
    );
    return `\n\n${token}\n\n`;
  });

  return withTokens
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => {
      if (imageMap.has(block)) {
        return imageMap.get(block) as string;
      }
      return `<p>${escapeHtml(block).replaceAll("\n", "<br />")}</p>`;
    })
    .join("");
}

function hasBrokenImageReference(question: ReviewQuestionDetail) {
  const values = [
    question.imageUrl,
    question.statement,
    question.statementHtml,
    ...question.assets.flatMap((asset) => [asset.url, asset.storagePath]),
    ...question.alternatives.flatMap((alternative) =>
      alternative.assets.flatMap((asset) => [asset.url, asset.storagePath]),
    ),
  ];

  return values.some((value) => value?.toLowerCase().includes("broken-image"));
}

function requiresVisualAsset(question: Pick<ReviewQuestionDetail, "statement" | "statementHtml" | "assetsCount">) {
  const content = `${question.statement} ${question.statementHtml ?? ""}`.toLowerCase();
  return visualKeywords.some((keyword) => content.includes(keyword)) && question.assetsCount === 0;
}

function hasSuspiciousText(question: Pick<ReviewQuestionDetail, "title" | "statement" | "statementHtml">) {
  const content = `${question.title} ${question.statement} ${question.statementHtml ?? ""}`;
  const hasMarker = mojibakeMarkers.some((marker) => content.includes(marker));
  const hasCyrillic = /[\u0400-\u04FF]/.test(content);
  return hasMarker || hasCyrillic;
}

function buildAlerts(question: ReviewQuestionDetail) {
  const alerts: Array<{ tone: "danger" | "warning"; text: string }> = [];

  if (hasBrokenImageReference(question)) {
    alerts.push({
      tone: "danger",
      text: "Imagem quebrada detectada no enunciado ou nos assets. Esta questao nao pode ser publicada ainda.",
    });
  }
  if (requiresVisualAsset(question)) {
    alerts.push({
      tone: "warning",
      text: "O texto menciona recurso visual, mas assetsCount esta zerado. Revisao manual obrigatoria.",
    });
  }
  if (question.sourceBookColor === "UNKNOWN") {
    alerts.push({
      tone: "warning",
      text: "Caderno de origem ainda esta como UNKNOWN. Confirme a cor oficial antes da publicacao.",
    });
  }
  if (!question.validatedAgainstOfficialSource) {
    alerts.push({
      tone: "warning",
      text: "Questao ainda nao foi validada contra a fonte oficial do INEP.",
    });
  }
  if (hasSuspiciousText(question)) {
    alerts.push({
      tone: "warning",
      text: "Texto possivelmente quebrado detectado. Revise caracteres, OCR e integridade do enunciado.",
    });
  }

  return alerts;
}

function canPublish(question: ReviewQuestionDetail) {
  return (
    question.validatedAgainstOfficialSource &&
    question.alternativesCount === 5 &&
    Boolean(question.correctAlternative) &&
    !hasBrokenImageReference(question)
  );
}

function ReviewCard({
  item,
  selected,
  onClick,
}: {
  item: ReviewQuestionSummary;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "w-full rounded-[24px] border p-4 text-left transition",
        selected
          ? "border-primary bg-primary/5 shadow-lg shadow-blue-500/10"
          : "border-border/70 bg-background/60 hover:bg-accent",
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-2">
          <p className="text-sm font-semibold">{item.title}</p>
          <div className="flex flex-wrap gap-2">
            <Badge variant={statusBadgeVariant(item.importStatus)}>{item.importStatus}</Badge>
            <Badge variant="outline">{item.source}</Badge>
            <Badge variant="outline">{item.subject}</Badge>
          </div>
        </div>
        <Badge variant={item.validatedAgainstOfficialSource ? "success" : "warning"}>
          {item.validatedAgainstOfficialSource ? "Fonte validada" : "Pendente INEP"}
        </Badge>
      </div>
      <div className="mt-4 grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
        <p>Ano: {item.sourceYear ?? "-"}</p>
        <p>Questao: {item.sourceQuestionNumber ?? "-"}</p>
        <p>Alternativas: {item.alternativesCount}</p>
        <p>Assets: {item.assetsCount}</p>
      </div>
    </button>
  );
}

export function ImportReviewAdminView() {
  useProtectedRoute();

  const user = useAuthStore((state) => state.user);
  const hydrated = useAuthStore((state) => state.hydrated);
  const isAdmin = user?.roles.includes("ROLE_ADMIN") ?? false;

  const [filters, setFilters] = useState<ReviewQuestionFilters>({
    page: 0,
    size: 10,
    sortBy: "createdAt",
    direction: "DESC",
    status: "",
    source: "ENEM_DEV",
    year: "",
    subject: "",
  });
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [validationForm, setValidationForm] = useState<ReviewOfficialValidationPayload>({
    officialSourceUrl: "",
    officialPdfUrl: "",
    officialAnswerKeyUrl: "",
    officialPage: null,
  });

  const reviewQuery = useReviewQuestions(filters);
  const detailQuery = useReviewQuestion(selectedId);
  const updateStatus = useUpdateReviewStatus();
  const validateOfficial = useValidateOfficialSource();
  const publishQuestion = usePublishReviewQuestion();

  useEffect(() => {
    if (!reviewQuery.data?.items.length) {
      setSelectedId(null);
      return;
    }

    if (!selectedId || !reviewQuery.data.items.some((item) => item.id === selectedId)) {
      setSelectedId(reviewQuery.data.items[0].id);
    }
  }, [reviewQuery.data?.items, selectedId]);

  useEffect(() => {
    if (!detailQuery.data) {
      return;
    }

    setValidationForm({
      officialSourceUrl: detailQuery.data.officialSourceUrl ?? "",
      officialPdfUrl: detailQuery.data.officialPdfUrl ?? "",
      officialAnswerKeyUrl: detailQuery.data.officialAnswerKeyUrl ?? "",
      officialPage: detailQuery.data.officialPage,
    });
  }, [detailQuery.data]);

  const alerts = useMemo(
    () => (detailQuery.data ? buildAlerts(detailQuery.data) : []),
    [detailQuery.data],
  );

  if (hydrated && !isAdmin) {
    return (
      <ErrorState
        title="Acesso restrito"
        description="Esta area de revisao de importacao esta disponivel apenas para administradores."
      />
    );
  }

  const selectedQuestion = detailQuery.data;
  const renderableStatementHtml =
    selectedQuestion?.statementHtml ?? markdownToHtml(selectedQuestion?.statement ?? null);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Revisao admin"
        title="Auditoria de questoes importadas"
        description="Revise itens vindos do ENEM_DEV e de futuras esteiras oficiais antes de qualquer publicacao para alunos."
      />

      <Card className="overflow-hidden">
        <CardHeader className="border-b border-border/70 bg-gradient-to-r from-primary/10 via-background to-secondary/10">
          <CardTitle className="flex items-center gap-2">
            <FileSearch className="size-5 text-primary" />
            Filtros de revisao
          </CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 pt-6 md:grid-cols-2 xl:grid-cols-4">
          <div className="space-y-2">
            <Label htmlFor="status">Status</Label>
            <select
              id="status"
              className="flex h-11 w-full rounded-2xl border border-border bg-background/70 px-4 py-2 text-sm"
              value={filters.status ?? ""}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  page: 0,
                  status: event.target.value as QuestionImportStatus | "",
                }))
              }
            >
              {statusOptions.map((option) => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="source">Fonte</Label>
            <Input
              id="source"
              value={filters.source ?? ""}
              onChange={(event) =>
                setFilters((current) => ({ ...current, page: 0, source: event.target.value }))
              }
              placeholder="ENEM_DEV"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="year">Ano</Label>
            <Input
              id="year"
              type="number"
              value={filters.year === "" ? "" : (filters.year ?? "")}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  page: 0,
                  year: event.target.value ? Number(event.target.value) : "",
                }))
              }
              placeholder="2023"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="subject">Assunto</Label>
            <Input
              id="subject"
              value={filters.subject ?? ""}
              onChange={(event) =>
                setFilters((current) => ({ ...current, page: 0, subject: event.target.value }))
              }
              placeholder="Linguagens"
            />
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[420px_minmax(0,1fr)]">
        <Card className="overflow-hidden">
          <CardHeader className="border-b border-border/70">
            <div className="flex items-center justify-between gap-3">
              <CardTitle>Fila de revisao</CardTitle>
              {reviewQuery.data ? (
                <Badge variant="outline">
                  {reviewQuery.data.metadata.totalElements} itens
                </Badge>
              ) : null}
            </div>
          </CardHeader>
          <CardContent className="space-y-4 pt-6">
            {reviewQuery.isError ? (
              <ErrorState
                title="Nao foi possivel carregar a fila de revisao."
                description="Tente novamente para buscar o estado mais recente do backend."
                onRetry={() => void reviewQuery.refetch()}
              />
            ) : reviewQuery.data?.items.length ? (
              <>
                <div className="space-y-3">
                  {reviewQuery.data.items.map((item) => (
                    <ReviewCard
                      key={item.id}
                      item={item}
                      selected={item.id === selectedId}
                      onClick={() => setSelectedId(item.id)}
                    />
                  ))}
                </div>

                <div className="flex items-center justify-between gap-3 border-t border-border/70 pt-4 text-sm text-muted-foreground">
                  <p>
                    Pagina {reviewQuery.data.metadata.page + 1} de{" "}
                    {Math.max(reviewQuery.data.metadata.totalPages, 1)}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={reviewQuery.data.metadata.first}
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
                      size="sm"
                      disabled={reviewQuery.data.metadata.last}
                      onClick={() =>
                        setFilters((current) => ({
                          ...current,
                          page: (current.page ?? 0) + 1,
                        }))
                      }
                    >
                      Proxima
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <EmptyState
                title="Nenhuma questao encontrada"
                description="Ajuste os filtros para localizar itens em revisao, validacao ou auditoria."
              />
            )}
          </CardContent>
        </Card>

        <div className="space-y-6">
          {selectedId === null ? (
            <EmptyState
              title="Selecione uma questao"
              description="Escolha um item da fila para revisar o enunciado, os assets e as informacoes de auditoria."
            />
          ) : detailQuery.isError ? (
            <ErrorState
              title="Nao foi possivel carregar o detalhe da revisao."
              description="Tente novamente para abrir a questao selecionada."
              onRetry={() => void detailQuery.refetch()}
            />
          ) : selectedQuestion ? (
            <>
              <Card className="overflow-hidden">
                <CardHeader className="border-b border-border/70 bg-gradient-to-r from-background via-background to-primary/5">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="space-y-3">
                      <div className="flex flex-wrap gap-2">
                        <Badge variant={statusBadgeVariant(selectedQuestion.importStatus)}>
                          {selectedQuestion.importStatus}
                        </Badge>
                        <Badge variant="outline">{selectedQuestion.source}</Badge>
                        <Badge variant="outline">{selectedQuestion.subject}</Badge>
                        <Badge
                          variant={
                            selectedQuestion.validatedAgainstOfficialSource
                              ? "success"
                              : "warning"
                          }
                        >
                          {selectedQuestion.validatedAgainstOfficialSource
                            ? "Fonte oficial validada"
                            : "Pendente INEP"}
                        </Badge>
                      </div>
                      <CardTitle className="text-2xl">{selectedQuestion.title}</CardTitle>
                      <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                        <p>Ano: {selectedQuestion.sourceYear ?? "-"}</p>
                        <p>Numero: {selectedQuestion.sourceQuestionNumber ?? "-"}</p>
                        <p>Caderno: {selectedQuestion.sourceBookColor ?? "-"}</p>
                        <p>Dia: {selectedQuestion.sourceDay ?? "-"}</p>
                        <p>Import batch: {selectedQuestion.importBatchId ?? "-"}</p>
                        <p>Dificuldade: {selectedQuestion.difficulty}</p>
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button
                        variant="outline"
                        onClick={() =>
                          updateStatus.mutate({
                            id: selectedQuestion.id,
                            payload: { importStatus: "NEEDS_REVIEW" },
                          })
                        }
                        disabled={updateStatus.isPending}
                      >
                        Manter em revisao
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() =>
                          updateStatus.mutate({
                            id: selectedQuestion.id,
                            payload: { importStatus: "INVALID" },
                          })
                        }
                        disabled={updateStatus.isPending}
                      >
                        Marcar como invalida
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() =>
                          updateStatus.mutate({
                            id: selectedQuestion.id,
                            payload: { importStatus: "VALIDATED" },
                          })
                        }
                        disabled={updateStatus.isPending}
                      >
                        Validar
                      </Button>
                      <Button
                        onClick={() => {
                          if (!window.confirm("Publicar esta questao para os alunos?")) {
                            return;
                          }
                          publishQuestion.mutate(selectedQuestion.id);
                        }}
                        disabled={!canPublish(selectedQuestion) || publishQuestion.isPending}
                      >
                        Publicar
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-6 pt-6">
                  {alerts.length ? (
                    <div className="space-y-3">
                      {alerts.map((alert) => (
                        <div
                          key={alert.text}
                          className={cn(
                            "flex gap-3 rounded-[22px] border p-4 text-sm",
                            alert.tone === "danger"
                              ? "border-rose-500/30 bg-rose-500/10 text-rose-700 dark:text-rose-300"
                              : "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300",
                          )}
                        >
                          {alert.tone === "danger" ? (
                            <AlertTriangle className="mt-0.5 size-4 shrink-0" />
                          ) : (
                            <ShieldAlert className="mt-0.5 size-4 shrink-0" />
                          )}
                          <p>{alert.text}</p>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="flex items-start gap-3 rounded-[22px] border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
                      <p>Nenhum alerta automatico encontrado nesta revisao.</p>
                    </div>
                  )}

                  <div className="grid gap-6 2xl:grid-cols-[minmax(0,1fr)_360px]">
                    <div className="space-y-6">
                      <Card className="border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Enunciado completo</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                          <QuestionContent
                            statement={selectedQuestion.statement}
                            statementHtml={renderableStatementHtml}
                            assets={selectedQuestion.assets}
                            sourceLabel="Assets vinculados"
                          />
                          <Textarea value={selectedQuestion.statement} readOnly className="min-h-36" />
                        </CardContent>
                      </Card>

                      <Card className="border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Alternativas e gabarito</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                          <div className="flex flex-wrap gap-2">
                            <Badge variant="outline">
                              Gabarito: {selectedQuestion.correctAlternative ?? "-"}
                            </Badge>
                            <Badge variant="outline">
                              Alternatives: {selectedQuestion.alternativesCount}
                            </Badge>
                          </div>
                          <div className="space-y-4">
                            {selectedQuestion.alternatives.map((alternative) => (
                              <div
                                key={alternative.id}
                                className="rounded-[22px] border border-border/70 bg-background/70 p-4"
                              >
                                <p className="mb-2 text-sm font-semibold">
                                  Alternativa {alternative.letter}
                                </p>
                                <AlternativeContent alternative={alternative} />
                              </div>
                            ))}
                          </div>
                          {selectedQuestion.explanation ? (
                            <div className="rounded-[22px] border border-border/70 bg-background/70 p-4">
                              <p className="text-sm font-semibold">Explicacao</p>
                              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                                {selectedQuestion.explanation}
                              </p>
                            </div>
                          ) : null}
                        </CardContent>
                      </Card>
                    </div>

                    <div className="space-y-6">
                      <Card className="border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Auditoria e origem</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4 text-sm">
                          <div className="space-y-2">
                            <p className="font-semibold">Origem externa</p>
                            <p className="text-muted-foreground">
                              Provider: {selectedQuestion.externalProvider ?? "-"}
                            </p>
                            {selectedQuestion.sourceUrl ? (
                              <Link
                                href={selectedQuestion.sourceUrl}
                                target="_blank"
                                className="inline-flex items-center gap-1 text-primary"
                              >
                                Abrir sourceUrl
                                <ExternalLink className="size-3.5" />
                              </Link>
                            ) : null}
                          </div>
                          <div className="space-y-2 border-t border-border/70 pt-4">
                            <p className="font-semibold">Fonte oficial</p>
                            <p className="text-muted-foreground">
                              officialSourceUrl: {selectedQuestion.officialSourceUrl ?? "-"}
                            </p>
                            <p className="text-muted-foreground">
                              officialPdfUrl: {selectedQuestion.officialPdfUrl ?? "-"}
                            </p>
                            <p className="text-muted-foreground">
                              officialAnswerKeyUrl: {selectedQuestion.officialAnswerKeyUrl ?? "-"}
                            </p>
                            <p className="text-muted-foreground">
                              Pagina oficial: {selectedQuestion.officialPage ?? "-"}
                            </p>
                          </div>
                          <div className="space-y-2 border-t border-border/70 pt-4">
                            <p className="font-semibold">Checks rapidos</p>
                            <p className="text-muted-foreground">
                              statementHash: {selectedQuestion.statementHash}
                            </p>
                            <p className="text-muted-foreground">
                              validatedAt: {selectedQuestion.validatedAt ?? "-"}
                            </p>
                            <p className="text-muted-foreground">
                              assetsCount: {selectedQuestion.assetsCount}
                            </p>
                          </div>
                        </CardContent>
                      </Card>

                      <Card className="border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Validacao manual com INEP</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                          <div className="space-y-2">
                            <Label htmlFor="officialSourceUrl">officialSourceUrl</Label>
                            <Input
                              id="officialSourceUrl"
                              value={validationForm.officialSourceUrl ?? ""}
                              onChange={(event) =>
                                setValidationForm((current) => ({
                                  ...current,
                                  officialSourceUrl: event.target.value,
                                }))
                              }
                              placeholder="https://www.gov.br/inep/..."
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="officialPdfUrl">officialPdfUrl</Label>
                            <Input
                              id="officialPdfUrl"
                              value={validationForm.officialPdfUrl ?? ""}
                              onChange={(event) =>
                                setValidationForm((current) => ({
                                  ...current,
                                  officialPdfUrl: event.target.value,
                                }))
                              }
                              placeholder="PDF oficial da prova"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="officialAnswerKeyUrl">officialAnswerKeyUrl</Label>
                            <Input
                              id="officialAnswerKeyUrl"
                              value={validationForm.officialAnswerKeyUrl ?? ""}
                              onChange={(event) =>
                                setValidationForm((current) => ({
                                  ...current,
                                  officialAnswerKeyUrl: event.target.value,
                                }))
                              }
                              placeholder="Gabarito oficial"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="officialPage">Pagina oficial</Label>
                            <Input
                              id="officialPage"
                              type="number"
                              value={validationForm.officialPage ?? ""}
                              onChange={(event) =>
                                setValidationForm((current) => ({
                                  ...current,
                                  officialPage: event.target.value ? Number(event.target.value) : null,
                                }))
                              }
                              placeholder="14"
                            />
                          </div>
                          <Button
                            className="w-full"
                            variant="secondary"
                            onClick={() =>
                              validateOfficial.mutate({
                                id: selectedQuestion.id,
                                payload: validationForm,
                              })
                            }
                            disabled={validateOfficial.isPending}
                          >
                            Validar contra fonte oficial
                          </Button>
                        </CardContent>
                      </Card>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </>
          ) : (
            <Card>
              <CardContent className="p-6">Carregando detalhe...</CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
