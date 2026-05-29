"use client";

import { AlertTriangle, ArrowLeft, ArrowRight, CheckCircle2, ChevronDown, ExternalLink, FileSearch, ShieldAlert } from "lucide-react";
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
  useAutoPublishSafe,
  useAutoValidateBatch,
  useAutoValidateQuestion,
  useCreateOfficialSource,
  useDeleteOfficialSource,
  useOfficialSources,
  usePublishReviewQuestion,
  useRecoverAssets,
  useRecoverAssetsBatch,
  useReviewQuestion,
  useReviewCounters,
  useReviewQuestions,
  useUpdateReviewStatus,
  useValidateAgainstOfficialSource,
  useValidateAgainstOfficialSourceBatch,
  useValidateOfficialSource,
} from "@/hooks/use-admin-import-review";
import { useProtectedRoute } from "@/hooks/use-protected-route";
import { useAuthStore } from "@/store/auth-store";
import type {
  AutoValidationStatus,
  OfficialExamSourcePayload,
  OfficialValidationReport,
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
  { value: "AUTO_VALIDATED", label: "Auto validated" },
  { value: "INVALID", label: "Invalid" },
  { value: "PUBLISHED", label: "Published" },
];

const autoValidationOptions: Array<{ value: AutoValidationStatus | ""; label: string }> = [
  { value: "", label: "Todos os status automaticos" },
  { value: "SAFE_TO_AUTO_VALIDATE", label: "Seguras" },
  { value: "NEEDS_HUMAN_REVIEW", label: "Revisao humana" },
  { value: "AUTO_INVALID", label: "Auto invalidas" },
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
    case "AUTO_VALIDATED":
      return "secondary";
    case "INVALID":
      return "danger";
    case "NEEDS_REVIEW":
      return "warning";
    default:
      return "outline";
  }
}

function autoStatusBadgeVariant(status: AutoValidationStatus) {
  switch (status) {
    case "SAFE_TO_AUTO_VALIDATE":
      return "success";
    case "AUTO_INVALID":
      return "danger";
    default:
      return "warning";
  }
}

function splitMessages(value?: string | null) {
  return value?.split(/\n+/).map((item) => item.trim()).filter(Boolean) ?? [];
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
    const isBroken = safeSrc.toLowerCase().includes("broken-image");
    imageMap.set(
      token,
      isBroken
        ? `<div class="rounded-[24px] border border-dashed border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-700 dark:text-amber-300"><strong>Imagem indisponivel</strong><p class="mt-2">${safeAlt}</p><p class="mt-1 break-all text-xs">${safeSrc}</p></div>`
        : `<figure class="space-y-2 rounded-[24px] border border-border/70 bg-background/70 p-3"><img src="${safeSrc}" alt="${safeAlt}" class="max-h-[420px] w-full object-contain rounded-[18px] bg-muted/20" /><figcaption class="text-xs text-muted-foreground">${safeAlt}</figcaption></figure>`,
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

  if (hasBrokenImageReference(question) || question.brokenImageDetected) {
    alerts.push({
      tone: "danger",
      text: "Imagem quebrada detectada no enunciado ou nos assets. Esta questao nao pode ser publicada ainda.",
    });
  }
  if (requiresVisualAsset(question) || question.requiresAssetReview) {
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
  if (hasSuspiciousText(question) || question.suspiciousTextDetected) {
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
    !hasBrokenImageReference(question) &&
    !question.brokenImageDetected &&
    !question.suspiciousTextDetected &&
    !question.requiresAssetReview
  );
}

function QuestionRawText({ value }: { value: string }) {
  return (
    <details className="rounded-[22px] border border-border/70 bg-background/60">
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 text-sm font-semibold">
        Ver texto bruto
        <ChevronDown className="size-4 text-muted-foreground" />
      </summary>
      <div className="border-t border-border/70 p-4">
        <Textarea value={value} readOnly className="min-h-40 text-xs leading-6" />
      </div>
    </details>
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
      <div className="mt-3 flex flex-wrap gap-2">
        <Badge variant={autoStatusBadgeVariant(item.autoValidationStatus)}>
          Score {item.autoValidationScore}
        </Badge>
        <Badge variant="outline">{item.autoValidationStatus}</Badge>
        {item.brokenImageDetected ? <Badge variant="danger">Imagem quebrada</Badge> : null}
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
    autoValidationStatus: "",
  });
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [validationForm, setValidationForm] = useState<ReviewOfficialValidationPayload>({
    officialSourceUrl: "",
    officialPdfUrl: "",
    officialAnswerKeyUrl: "",
    officialPage: null,
  });
  const [officialSourceForm, setOfficialSourceForm] = useState<OfficialExamSourcePayload>({
    exam: "ENEM",
    year: 2023,
    day: 1,
    bookColor: "",
    pdfUrl: "",
    answerKeyUrl: "",
    sourceUrl: "",
    localPdfPath: "",
    answerKeyMapJson: "",
  });
  const [officialValidationReport, setOfficialValidationReport] = useState<OfficialValidationReport | null>(null);

  const reviewQuery = useReviewQuestions(filters);
  const countersQuery = useReviewCounters();
  const officialSourcesQuery = useOfficialSources();
  const detailQuery = useReviewQuestion(selectedId);
  const updateStatus = useUpdateReviewStatus();
  const validateOfficial = useValidateOfficialSource();
  const publishQuestion = usePublishReviewQuestion();
  const autoValidateQuestion = useAutoValidateQuestion();
  const autoValidateBatch = useAutoValidateBatch();
  const autoPublishSafe = useAutoPublishSafe();
  const createOfficialSource = useCreateOfficialSource();
  const deleteOfficialSource = useDeleteOfficialSource();
  const recoverAssets = useRecoverAssets();
  const recoverAssetsBatch = useRecoverAssetsBatch();
  const validateWithInep = useValidateAgainstOfficialSource();
  const validateWithInepBatch = useValidateAgainstOfficialSourceBatch();

  const reviewItems = reviewQuery.data?.items ?? [];

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
  const autoWarnings = splitMessages(detailQuery.data?.autoValidationWarnings);
  const autoErrors = splitMessages(detailQuery.data?.autoValidationErrors);

  const selectedIndex = reviewItems.findIndex((item) => item.id === selectedId);
  const previousItem = selectedIndex > 0 ? reviewItems[selectedIndex - 1] : null;
  const nextItem = selectedIndex >= 0 && selectedIndex < reviewItems.length - 1
    ? reviewItems[selectedIndex + 1]
    : null;

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

  async function moveToNextQuestion(preferredId?: number | null) {
    if (preferredId) {
      setSelectedId(preferredId);
      return;
    }
    if (nextItem) {
      setSelectedId(nextItem.id);
      return;
    }
    if (reviewItems.length > 0) {
      setSelectedId(reviewItems[0].id);
    }
  }

  async function handleStatusAndContinue(importStatus: "NEEDS_REVIEW" | "INVALID" | "VALIDATED") {
    if (!selectedQuestion) {
      return;
    }
    const updated = await updateStatus.mutateAsync({
      id: selectedQuestion.id,
      payload: { importStatus },
    });
    await moveToNextQuestion(nextItem?.id ?? (updated.id === selectedQuestion.id ? null : updated.id));
  }

  async function handleValidateAndContinue() {
    if (!selectedQuestion) {
      return;
    }
    await validateOfficial.mutateAsync({
      id: selectedQuestion.id,
      payload: validationForm,
    });
    await moveToNextQuestion(nextItem?.id);
  }

  async function handleCreateOfficialSource() {
    await createOfficialSource.mutateAsync({
      ...officialSourceForm,
      bookColor: officialSourceForm.bookColor?.trim() || null,
      answerKeyUrl: officialSourceForm.answerKeyUrl?.trim() || null,
      localPdfPath: officialSourceForm.localPdfPath?.trim() || null,
      answerKeyMapJson: officialSourceForm.answerKeyMapJson?.trim() || null,
    });
  }

  async function handleValidateWithInep(questionId: number) {
    const result = await validateWithInep.mutateAsync(questionId);
    setOfficialValidationReport(result);
  }

  async function handleValidateWithInepBatch() {
    const result = await validateWithInepBatch.mutateAsync();
    setOfficialValidationReport(result);
  }

  async function handleRecoverAssets(questionId: number) {
    const result = await recoverAssets.mutateAsync(questionId);
    setOfficialValidationReport(result);
  }

  async function handleRecoverAssetsBatch() {
    const result = await recoverAssetsBatch.mutateAsync();
    setOfficialValidationReport(result);
  }

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
        <CardContent className="grid gap-4 pt-6 md:grid-cols-2 xl:grid-cols-5">
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
          <div className="space-y-2">
            <Label htmlFor="autoValidationStatus">Auto validacao</Label>
            <select
              id="autoValidationStatus"
              className="flex h-11 w-full rounded-2xl border border-border bg-background/70 px-4 py-2 text-sm"
              value={filters.autoValidationStatus ?? ""}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  page: 0,
                  autoValidationStatus: event.target.value as AutoValidationStatus | "",
                }))
              }
            >
              {autoValidationOptions.map((option) => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-3 md:grid-cols-5">
        {[
          ["Seguras", countersQuery.data?.safe ?? 0],
          ["Em revisao", countersQuery.data?.needsReview ?? 0],
          ["Invalidas", countersQuery.data?.invalid ?? 0],
          ["Imagem quebrada", countersQuery.data?.brokenImages ?? 0],
          ["Pendentes INEP", countersQuery.data?.pendingInep ?? 0],
        ].map(([label, value]) => (
          <div key={label} className="rounded-[20px] border border-border/70 bg-background/70 p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              {label}
            </p>
            <p className="mt-2 text-2xl font-bold">{value}</p>
          </div>
        ))}
      </div>

      <Card className="overflow-hidden">
        <CardHeader className="border-b border-border/70">
          <CardTitle>Fontes oficiais INEP cadastradas</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-6 pt-6 xl:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
          <div className="space-y-3">
            {officialSourcesQuery.data?.length ? (
              officialSourcesQuery.data.slice(0, 5).map((source) => (
                <div key={source.id} className="rounded-[20px] border border-border/70 bg-background/70 p-4 text-sm">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="outline">{source.exam} {source.year}</Badge>
                    <Badge variant="outline">Dia {source.day ?? "-"}</Badge>
                    <Badge variant="outline">{source.bookColor || "cor livre"}</Badge>
                    {source.answerKeyMapJson ? <Badge variant="success">Gabarito estruturado</Badge> : <Badge variant="warning">Sem mapa de gabarito</Badge>}
                  </div>
                  <p className="mt-3 break-all text-muted-foreground">PDF: {source.pdfUrl}</p>
                  <p className="mt-1 break-all text-muted-foreground">Gabarito: {source.answerKeyUrl ?? "-"}</p>
                  <Button
                    className="mt-3"
                    variant="outline"
                    onClick={() => {
                      if (!window.confirm("Remover esta fonte oficial?")) {
                        return;
                      }
                      deleteOfficialSource.mutate(source.id);
                    }}
                    disabled={deleteOfficialSource.isPending}
                  >
                    Remover
                  </Button>
                </div>
              ))
            ) : (
              <p className="rounded-[20px] border border-dashed border-border/70 p-4 text-sm text-muted-foreground">
                Nenhuma fonte oficial cadastrada ainda. Cadastre a prova/gabarito do INEP antes de validar automaticamente.
              </p>
            )}
          </div>

          <div className="space-y-3 rounded-[22px] border border-border/70 bg-background/70 p-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Exam</Label>
                <Input value={officialSourceForm.exam} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, exam: event.target.value }))} />
              </div>
              <div className="space-y-2">
                <Label>Ano</Label>
                <Input type="number" value={officialSourceForm.year} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, year: Number(event.target.value) }))} />
              </div>
              <div className="space-y-2">
                <Label>Dia</Label>
                <Input type="number" value={officialSourceForm.day ?? ""} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, day: event.target.value ? Number(event.target.value) : null }))} />
              </div>
              <div className="space-y-2">
                <Label>Caderno/cor</Label>
                <Input value={officialSourceForm.bookColor ?? ""} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, bookColor: event.target.value }))} placeholder="AZUL" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>pdfUrl oficial</Label>
              <Input value={officialSourceForm.pdfUrl} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, pdfUrl: event.target.value }))} placeholder="https://www.gov.br/inep/..." />
            </div>
            <div className="space-y-2">
              <Label>answerKeyUrl oficial</Label>
              <Input value={officialSourceForm.answerKeyUrl ?? ""} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, answerKeyUrl: event.target.value }))} />
            </div>
            <div className="space-y-2">
              <Label>sourceUrl oficial</Label>
              <Input value={officialSourceForm.sourceUrl} onChange={(event) => setOfficialSourceForm((current) => ({ ...current, sourceUrl: event.target.value }))} />
            </div>
            <div className="space-y-2">
              <Label>Mapa de gabarito JSON opcional</Label>
              <Textarea
                value={officialSourceForm.answerKeyMapJson ?? ""}
                onChange={(event) => setOfficialSourceForm((current) => ({ ...current, answerKeyMapJson: event.target.value }))}
                placeholder='{"2":"A","3":"B"}'
              />
            </div>
            <Button className="w-full" onClick={() => void handleCreateOfficialSource()} disabled={createOfficialSource.isPending}>
              Cadastrar fonte oficial
            </Button>
          </div>
        </CardContent>
      </Card>

      {officialValidationReport ? (
        <Card className="overflow-hidden">
          <CardHeader className="border-b border-border/70">
            <CardTitle>Relatorio da validacao INEP</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 pt-6">
            <div className="grid gap-3 md:grid-cols-4 xl:grid-cols-8">
              {[
                ["Processadas", officialValidationReport.totalProcessed],
                ["Validadas", officialValidationReport.validated],
                ["Atualizadas", officialValidationReport.updatedQuestions],
                ["Falhas", officialValidationReport.failed],
                ["Ambiguas", officialValidationReport.ambiguousOfficialSource],
                ["Gabarito ausente", officialValidationReport.answerKeyMissing],
                ["Gabarito divergente", officialValidationReport.answerKeyMismatch],
                ["Pendentes INEP", officialValidationReport.pendingInep],
              ].map(([label, value]) => (
                <div key={label} className="rounded-[18px] border border-border/70 bg-background/70 p-3">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">{label}</p>
                  <p className="mt-2 text-xl font-bold">{value}</p>
                </div>
              ))}
            </div>

            <div className="max-h-[320px] space-y-2 overflow-auto rounded-[20px] border border-border/70 p-3">
              {officialValidationReport.items.map((item) => (
                <div key={`${item.questionId}-${item.sourceQuestionNumber}`} className="rounded-[16px] bg-muted/40 p-3 text-sm">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant={item.updated ? "success" : "warning"}>
                      {item.updated ? "Atualizada" : "Sem mudanca"}
                    </Badge>
                    <Badge variant={item.newValidatedAgainstOfficialSource ? "success" : "warning"}>
                      {item.newValidatedAgainstOfficialSource ? "INEP validado" : "Pendente INEP"}
                    </Badge>
                    <span className="font-semibold">#{item.sourceQuestionNumber ?? "-"} {item.title}</span>
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">
                    Score: {item.previousScore ?? "-"} {"->"} {item.newScore ?? "-"} | Validacao:{" "}
                    {String(item.previousValidatedAgainstOfficialSource)} {"->"} {String(item.newValidatedAgainstOfficialSource)}
                  </p>
                  {item.warnings.length ? (
                    <p className="mt-2 break-words text-xs text-amber-600">Warnings: {item.warnings.join(" | ")}</p>
                  ) : null}
                  {item.errors.length ? (
                    <p className="mt-2 break-words text-xs text-rose-600">Errors: {item.errors.join(" | ")}</p>
                  ) : null}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}

      <div className="grid items-start gap-6 xl:grid-cols-[380px_minmax(0,1fr)] 2xl:grid-cols-[420px_minmax(0,1fr)]">
        <Card className="overflow-hidden xl:sticky xl:top-24">
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
                  {reviewItems.map((item) => (
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

        <div className="min-w-0 space-y-6">
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
              <Card className="min-w-0 overflow-hidden">
                <CardHeader className="border-b border-border/70 bg-gradient-to-r from-background via-background to-primary/5">
                  <div className="flex flex-col gap-4">
                    <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                      <div className="space-y-3 min-w-0">
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
                        <CardTitle className="break-words text-2xl">{selectedQuestion.title}</CardTitle>
                        <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2 xl:grid-cols-3">
                          <p>Ano: {selectedQuestion.sourceYear ?? "-"}</p>
                          <p>Numero: {selectedQuestion.sourceQuestionNumber ?? "-"}</p>
                          <p>Caderno: {selectedQuestion.sourceBookColor ?? "-"}</p>
                          <p>Dia: {selectedQuestion.sourceDay ?? "-"}</p>
                          <p>Import batch: {selectedQuestion.importBatchId ?? "-"}</p>
                          <p>Dificuldade: {selectedQuestion.difficulty}</p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2 xl:max-w-[420px] xl:justify-end">
                        <Button
                          variant="outline"
                          onClick={() => previousItem && setSelectedId(previousItem.id)}
                          disabled={!previousItem}
                        >
                          <ArrowLeft className="size-4" />
                          Anterior
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => nextItem && setSelectedId(nextItem.id)}
                          disabled={!nextItem}
                        >
                          Proxima
                          <ArrowRight className="size-4" />
                        </Button>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
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
                          onClick={() => void handleStatusAndContinue("NEEDS_REVIEW")}
                          disabled={updateStatus.isPending}
                        >
                          Salvar e proxima
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => void handleStatusAndContinue("INVALID")}
                          disabled={updateStatus.isPending}
                        >
                          Marcar invalida e proxima
                        </Button>
                        <Button
                          variant="secondary"
                          onClick={() => void handleValidateAndContinue()}
                          disabled={validateOfficial.isPending}
                        >
                          Validar e continuar
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => autoValidateQuestion.mutate(selectedQuestion.id)}
                          disabled={autoValidateQuestion.isPending}
                        >
                          Auto validar
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => void handleValidateWithInep(selectedQuestion.id)}
                          disabled={validateWithInep.isPending}
                        >
                          Validar com INEP
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => void handleRecoverAssets(selectedQuestion.id)}
                          disabled={recoverAssets.isPending}
                        >
                          Recuperar assets
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

                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="outline"
                      onClick={() => autoValidateBatch.mutate()}
                      disabled={autoValidateBatch.isPending}
                    >
                      Auto validar lote
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => void handleValidateWithInepBatch()}
                      disabled={validateWithInepBatch.isPending}
                    >
                      Validar lote com INEP
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => void handleRecoverAssetsBatch()}
                      disabled={recoverAssetsBatch.isPending}
                    >
                      Recuperar assets do lote
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => autoPublishSafe.mutate()}
                      disabled={autoPublishSafe.isPending}
                    >
                      Publicar seguras
                    </Button>
                  </div>

                  <div className="grid min-w-0 items-start gap-6 2xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.85fr)]">
                    <div className="min-w-0 space-y-6">
                      <Card className="min-w-0 border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Enunciado completo</CardTitle>
                        </CardHeader>
                        <CardContent className="min-w-0 space-y-4">
                          <QuestionContent
                            statement={selectedQuestion.statement}
                            statementHtml={renderableStatementHtml}
                            assets={selectedQuestion.assets}
                            sourceLabel="Assets vinculados"
                          />
                          <QuestionRawText value={selectedQuestion.statement} />
                        </CardContent>
                      </Card>

                      <Card className="min-w-0 border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Alternativas e gabarito</CardTitle>
                        </CardHeader>
                        <CardContent className="min-w-0 space-y-4">
                          <div className="flex flex-wrap gap-2">
                            <Badge variant="outline">
                              Gabarito: {selectedQuestion.correctAlternative ?? "-"}
                            </Badge>
                            <Badge variant="outline">
                              Alternatives: {selectedQuestion.alternativesCount}
                            </Badge>
                          </div>
                          <div className="flex flex-wrap gap-2">
                            <Badge variant={autoStatusBadgeVariant(selectedQuestion.autoValidationStatus)}>
                              Auto score {selectedQuestion.autoValidationScore}
                            </Badge>
                            <Badge variant="outline">{selectedQuestion.autoValidationStatus}</Badge>
                            {selectedQuestion.brokenImageDetected ? <Badge variant="danger">Imagem quebrada</Badge> : null}
                            {selectedQuestion.suspiciousTextDetected ? <Badge variant="warning">Texto suspeito</Badge> : null}
                            {selectedQuestion.requiresAssetReview ? <Badge variant="warning">Asset pendente</Badge> : null}
                          </div>
                          <div className="space-y-4">
                            {selectedQuestion.alternatives.map((alternative) => (
                              <div
                                key={alternative.id}
                                className="min-w-0 rounded-[22px] border border-border/70 bg-background/70 p-4"
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

                    <div className="min-w-0 space-y-6 2xl:sticky 2xl:top-24">
                      <Card className="min-w-0 border-border/70 bg-background/60">
                        <CardHeader>
                          <CardTitle>Auditoria e origem</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4 text-sm">
                          <div className="grid gap-3">
                            <div className="rounded-[20px] border border-border/70 bg-background/70 p-4">
                              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Origem</p>
                              <div className="mt-3 space-y-2 text-muted-foreground">
                                <p className="break-all">source: {selectedQuestion.source}</p>
                                <p className="break-all">externalProvider: {selectedQuestion.externalProvider ?? "-"}</p>
                                <p>sourceBookColor: {selectedQuestion.sourceBookColor ?? "-"}</p>
                                <p>sourceDay: {selectedQuestion.sourceDay ?? "-"}</p>
                                <p>importBatchId: {selectedQuestion.importBatchId ?? "-"}</p>
                                {selectedQuestion.sourceUrl ? (
                                  <a
                                    href={selectedQuestion.sourceUrl}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-1 break-all text-primary"
                                  >
                                    Abrir sourceUrl
                                    <ExternalLink className="size-3.5" />
                                  </a>
                                ) : null}
                              </div>
                            </div>

                            <div className="rounded-[20px] border border-border/70 bg-background/70 p-4">
                              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Fonte oficial</p>
                              <div className="mt-3 space-y-2 text-muted-foreground">
                                <p>validatedAgainstOfficialSource: {String(selectedQuestion.validatedAgainstOfficialSource)}</p>
                                <p>validatedAt: {selectedQuestion.validatedAt ?? "-"}</p>
                                <p className="break-all">officialSourceUrl: {selectedQuestion.officialSourceUrl ?? "-"}</p>
                                <p className="break-all">officialPdfUrl: {selectedQuestion.officialPdfUrl ?? "-"}</p>
                                <p className="break-all">officialAnswerKeyUrl: {selectedQuestion.officialAnswerKeyUrl ?? "-"}</p>
                                <p>officialPage: {selectedQuestion.officialPage ?? "-"}</p>
                              </div>
                            </div>

                            <div className="rounded-[20px] border border-border/70 bg-background/70 p-4">
                              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Checks</p>
                              <div className="mt-3 space-y-2 text-muted-foreground">
                                <p className="break-all">statementHash: {selectedQuestion.statementHash}</p>
                                <p>alternativesCount: {selectedQuestion.alternativesCount}</p>
                                <p>assetsCount: {selectedQuestion.assetsCount}</p>
                                <p>autoValidationScore: {selectedQuestion.autoValidationScore}</p>
                                <p>autoValidationStatus: {selectedQuestion.autoValidationStatus}</p>
                                <p>autoValidatedAt: {selectedQuestion.autoValidatedAt ?? "-"}</p>
                              </div>
                            </div>
                            {(autoWarnings.length || autoErrors.length) ? (
                              <div className="rounded-[20px] border border-border/70 bg-background/70 p-4">
                                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Auto validacao</p>
                                {autoErrors.length ? (
                                  <div className="mt-3 space-y-1">
                                    <p className="text-sm font-semibold text-rose-600">Errors</p>
                                    {autoErrors.map((item) => (
                                      <p key={item} className="break-words text-muted-foreground">{item}</p>
                                    ))}
                                  </div>
                                ) : null}
                                {autoWarnings.length ? (
                                  <div className="mt-3 space-y-1">
                                    <p className="text-sm font-semibold text-amber-600">Warnings</p>
                                    {autoWarnings.map((item) => (
                                      <p key={item} className="break-words text-muted-foreground">{item}</p>
                                    ))}
                                  </div>
                                ) : null}
                              </div>
                            ) : null}
                          </div>
                      </CardContent>
                      </Card>

                      <Card className="min-w-0 border-border/70 bg-background/60">
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
