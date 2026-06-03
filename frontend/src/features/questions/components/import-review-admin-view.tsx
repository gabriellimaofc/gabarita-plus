"use client";

import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  ChevronDown,
  Copy,
  ExternalLink,
  FileSearch,
  ShieldAlert,
} from "lucide-react";
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
import { AlternativeContent, QuestionContent } from "@/features/questions/components/question-content";
import {
  useAutoPublishSafe,
  useAutoValidateBatch,
  useAutoValidateQuestion,
  useApproveReviewAsset,
  useCreateOfficialSource,
  useDeleteOfficialSource,
  useOfficialSources,
  usePublishReviewQuestion,
  useRemoveReviewAsset,
  useRecoverAssets,
  useRecoverAssetsBatch,
  useReviewCounters,
  useReviewQuestion,
  useReviewQuestions,
  useUpdateReviewAssetCrop,
  useUpdateReviewQuestion,
  useUpdateReviewStatus,
  useValidateAgainstOfficialSource,
  useValidateAgainstOfficialSourceBatch,
  useValidateOfficialSource,
} from "@/hooks/use-admin-import-review";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type {
  AutoValidationStatus,
  DifficultyLevel,
  OfficialExamSourcePayload,
  OfficialValidationReport,
  QuestionImportStatus,
  QuestionAsset,
  ReviewOfficialValidationPayload,
  ReviewQuestionDetail,
  ReviewQuestionAssetCropPayload,
  ReviewQuestionFilters,
  ReviewQuestionSummary,
  ReviewQuestionUpdatePayload,
} from "@/types/question";

const statusOptions: Array<{ value: QuestionImportStatus | ""; label: string }> = [
  { value: "", label: "Todos os status em revisão" },
  { value: "NEEDS_REVIEW", label: "Needs review" },
  { value: "DRAFT", label: "Draft" },
  { value: "VALIDATED", label: "Validated" },
  { value: "AUTO_VALIDATED", label: "Auto validated" },
  { value: "INVALID", label: "Invalid" },
  { value: "PUBLISHED", label: "Published" },
];

const autoValidationOptions: Array<{ value: AutoValidationStatus | ""; label: string }> = [
  { value: "", label: "Todos os status automáticos" },
  { value: "SAFE_TO_AUTO_VALIDATE", label: "Seguras" },
  { value: "NEEDS_HUMAN_REVIEW", label: "Revisão humana" },
  { value: "AUTO_INVALID", label: "Auto inválidas" },
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

const mojibakeMarkers = ["Ãƒ", "Ã¢â‚¬Å“", "Ã¢â‚¬Â", "Ã¢â‚¬â€œ", "Ð¢Ð•Ð¥Ð¢Ðž", "Ñ‚ÐµÐºÑÑ‚"];

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

function normalizeSuspiciousStatement(value: string) {
  return value.replaceAll("ТЕХТО", "TEXTO").replaceAll("Техто", "Texto").replaceAll("техто", "texto");
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
        ? `<div class="rounded-[24px] border border-dashed border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-700 dark:text-amber-300"><strong>Imagem indisponível</strong><p class="mt-2">${safeAlt}</p><p class="mt-1 break-words text-xs">${safeSrc}</p></div>`
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
      text: "Imagem quebrada detectada no enunciado ou nos assets. Esta questão não pode ser publicada ainda.",
    });
  }
  if (requiresVisualAsset(question) || question.requiresAssetReview) {
    alerts.push({
      tone: "warning",
      text: "Recorte recuperado do PDF oficial ou dependência visual pendente. Revisão manual obrigatória antes de publicar.",
    });
  }
  if (question.sourceBookColor === "UNKNOWN") {
    alerts.push({
      tone: "warning",
      text: "Caderno de origem ainda está como UNKNOWN. Confirme a cor oficial antes da publicação.",
    });
  }
  if (!question.validatedAgainstOfficialSource) {
    alerts.push({
      tone: "warning",
      text: "Questão ainda não foi validada contra a fonte oficial do INEP.",
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

function getPublishBlockers(question: ReviewQuestionDetail) {
  const blockers: string[] = [];

  if (question.importStatus === "NEEDS_REVIEW") {
    blockers.push("Questão ainda está em NEEDS_REVIEW");
  }
  if (!question.validatedAgainstOfficialSource) {
    blockers.push("Validação com INEP pendente");
  }
  if (question.autoValidationStatus === "NEEDS_HUMAN_REVIEW") {
    blockers.push("Auto validação exige revisão humana");
  }
  if (question.requiresAssetReview) {
    blockers.push("Asset recovery needs review");
  }
  if (question.suspiciousTextDetected) {
    blockers.push("Suspicious text detected");
  }
  if (hasBrokenImageReference(question) || question.brokenImageDetected) {
    blockers.push("Imagem quebrada detectada");
  }
  if (question.alternativesCount !== 5) {
    blockers.push(`Quantidade de alternativas fora do padrão (${question.alternativesCount}/5)`);
  }
  if (!question.correctAlternative) {
    blockers.push("Gabarito ausente");
  }

  return blockers;
}

function canPublish(question: ReviewQuestionDetail) {
  return getPublishBlockers(question).length === 0;
}

function formatBoolean(value: boolean | null | undefined) {
  if (value === true) {
    return "Sim";
  }
  if (value === false) {
    return "Não";
  }
  return "-";
}

async function copyToClipboard(value?: string | null) {
  if (!value) {
    return;
  }
  await navigator.clipboard.writeText(value);
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

function AuditValue({
  value,
  href,
  compact = false,
}: {
  value?: string | number | null;
  href?: string | null;
  compact?: boolean;
}) {
  const normalized = value === null || value === undefined || value === "" ? "-" : String(value);

  if (href) {
    return (
      <a
        href={href}
        target="_blank"
        rel="noreferrer"
        title={normalized}
        className={cn(
          "inline-flex min-w-0 max-w-full items-center gap-1 text-primary underline-offset-4 hover:underline",
          compact ? "truncate" : "break-words",
        )}
      >
        <span className={compact ? "truncate" : "break-words"}>{normalized}</span>
        <ExternalLink className="size-3.5 shrink-0" />
      </a>
    );
  }

  return (
    <span
      title={normalized}
      className={cn("block min-w-0 text-foreground", compact ? "truncate" : "break-words")}
    >
      {normalized}
    </span>
  );
}

function AuditField({
  label,
  value,
  href,
  compact = false,
}: {
  label: string;
  value?: string | number | null;
  href?: string | null;
  compact?: boolean;
}) {
  return (
    <div className="min-w-0 space-y-2 rounded-[18px] border border-border/60 bg-background/75 p-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <AuditValue value={value} href={href} compact={compact} />
    </div>
  );
}

function AuditUrlField({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  return (
    <div className="min-w-0 space-y-2 rounded-[18px] border border-border/60 bg-background/75 p-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <div className="min-w-0 space-y-3">
        <AuditValue value={value} href={value} />
        {value ? (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" onClick={() => void copyToClipboard(value)}>
              <Copy className="size-3.5" />
              Copiar
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.open(value, "_blank", "noopener,noreferrer")}
            >
              <ExternalLink className="size-3.5" />
              Abrir
            </Button>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function MiniStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-[20px] border border-border/70 bg-background/70 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-2 text-2xl font-bold">{value}</p>
    </div>
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
        {item.requiresAssetReview ? <Badge variant="warning">Asset pendente</Badge> : null}
        {item.brokenImageDetected ? <Badge variant="danger">Imagem quebrada</Badge> : null}
      </div>
      <div className="mt-4 grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
        <p>Ano: {item.sourceYear ?? "-"}</p>
        <p>Questão: {item.sourceQuestionNumber ?? "-"}</p>
        <p>Alternativas: {item.alternativesCount}</p>
        <p>Assets: {item.assetsCount}</p>
      </div>
    </button>
  );
}

export function ImportReviewAdminView() {
  const user = useAuthStore((state) => state.user);
  const hydrated = useAuthStore((state) => state.hydrated);
  const isAdmin = hydrated && (user?.roles.includes("ROLE_ADMIN") ?? false);

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
    cachedPdfUrl: "",
    cachedAnswerKeyUrl: "",
    answerKeyMapJson: "",
  });
  const [officialValidationReport, setOfficialValidationReport] = useState<OfficialValidationReport | null>(null);
  const [editForm, setEditForm] = useState<ReviewQuestionUpdatePayload>({
    statement: "",
    topic: "",
    subtopic: "",
    difficulty: "MEDIUM",
  });
  const [selectedAssetId, setSelectedAssetId] = useState<number | null>(null);
  const [cropForm, setCropForm] = useState<ReviewQuestionAssetCropPayload>({
    cropX: 0,
    cropY: 0,
    cropWidth: 1,
    cropHeight: 1,
  });

  const reviewQuery = useReviewQuestions(filters);
  const countersQuery = useReviewCounters();
  const officialSourcesQuery = useOfficialSources();
  const detailQuery = useReviewQuestion(selectedId);
  const updateStatus = useUpdateReviewStatus();
  const validateOfficial = useValidateOfficialSource();
  const publishQuestion = usePublishReviewQuestion();
  const updateReviewQuestion = useUpdateReviewQuestion();
  const removeReviewAsset = useRemoveReviewAsset();
  const updateReviewAssetCrop = useUpdateReviewAssetCrop();
  const approveReviewAsset = useApproveReviewAsset();
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
    setEditForm({
      statement: detailQuery.data.statement,
      topic: detailQuery.data.topic,
      subtopic: detailQuery.data.subtopic ?? "",
      difficulty: detailQuery.data.difficulty,
    });
    const firstAsset = detailQuery.data.assets[0] ?? null;
    setSelectedAssetId(firstAsset?.id ?? null);
    setCropForm({
      cropX: firstAsset?.cropX ?? 0,
      cropY: firstAsset?.cropY ?? 0,
      cropWidth: firstAsset?.cropWidth ?? 1,
      cropHeight: firstAsset?.cropHeight ?? 1,
    });
  }, [detailQuery.data]);

  useEffect(() => {
    const currentAsset =
      detailQuery.data?.assets.find((asset) => asset.id === selectedAssetId) ?? detailQuery.data?.assets[0] ?? null;
    if (!currentAsset) {
      return;
    }
    setCropForm({
      cropX: currentAsset.cropX ?? 0,
      cropY: currentAsset.cropY ?? 0,
      cropWidth: currentAsset.cropWidth ?? 1,
      cropHeight: currentAsset.cropHeight ?? 1,
    });
  }, [detailQuery.data, selectedAssetId]);

  const selectedIndex = reviewItems.findIndex((item) => item.id === selectedId);
  const previousItem = selectedIndex > 0 ? reviewItems[selectedIndex - 1] : null;
  const nextItem =
    selectedIndex >= 0 && selectedIndex < reviewItems.length - 1 ? reviewItems[selectedIndex + 1] : null;
  const selectedQuestion = detailQuery.data;
  const selectedAsset =
    selectedQuestion?.assets.find((asset) => asset.id === selectedAssetId) ?? selectedQuestion?.assets[0] ?? null;
  const alerts = useMemo(() => (selectedQuestion ? buildAlerts(selectedQuestion) : []), [selectedQuestion]);
  const autoWarnings = splitMessages(selectedQuestion?.autoValidationWarnings);
  const autoErrors = splitMessages(selectedQuestion?.autoValidationErrors);
  const publishBlockers = selectedQuestion ? getPublishBlockers(selectedQuestion) : [];
  const renderableStatementHtml =
    selectedQuestion?.statementHtml ?? markdownToHtml(selectedQuestion?.statement ?? null);

  if (hydrated && !isAdmin) {
    return (
      <ErrorState
        title="Acesso restrito"
        description="Esta área de revisão de importação está disponível apenas para administradores."
      />
    );
  }

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
      cachedPdfUrl: officialSourceForm.cachedPdfUrl?.trim() || null,
      cachedAnswerKeyUrl: officialSourceForm.cachedAnswerKeyUrl?.trim() || null,
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
        eyebrow="Revisão admin"
        title="Auditoria de questões importadas"
        description="Revise itens vindos do ENEM_DEV e de futuras esteiras oficiais antes de qualquer publicação para alunos."
      />

      <Card className="overflow-hidden">
        <CardHeader className="border-b border-border/70 bg-gradient-to-r from-primary/10 via-background to-secondary/10">
          <CardTitle className="flex items-center gap-2">
            <FileSearch className="size-5 text-primary" />
            Filtros de revisão
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
            <Label htmlFor="autoValidationStatus">Auto validação</Label>
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
        <MiniStat label="Seguras" value={countersQuery.data?.safe ?? 0} />
        <MiniStat label="Em revisão" value={countersQuery.data?.needsReview ?? 0} />
        <MiniStat label="Inválidas" value={countersQuery.data?.invalid ?? 0} />
        <MiniStat label="Imagem quebrada" value={countersQuery.data?.brokenImages ?? 0} />
        <MiniStat label="Pendentes INEP" value={countersQuery.data?.pendingInep ?? 0} />
      </div>

      {officialValidationReport ? (
        <Card className="overflow-hidden">
          <CardHeader className="border-b border-border/70">
            <CardTitle>Relatório da validação INEP</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 pt-6">
            <div className="grid gap-3 md:grid-cols-4 xl:grid-cols-9">
              {[
                ["Processadas", officialValidationReport.totalProcessed],
                ["Validadas", officialValidationReport.validated],
                ["Atualizadas", officialValidationReport.updatedQuestions],
                ["Falhas", officialValidationReport.failed],
                ["Ambíguas", officialValidationReport.ambiguousOfficialSource],
                ["Gabarito ausente", officialValidationReport.answerKeyMissing],
                ["Gabarito divergente", officialValidationReport.answerKeyMismatch],
                ["Pendentes INEP", officialValidationReport.pendingInep],
                ["Assets recuperados", officialValidationReport.assetRecovered],
              ].map(([label, value]) => (
                <div key={label} className="rounded-[18px] border border-border/70 bg-background/70 p-3">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                    {label}
                  </p>
                  <p className="mt-2 text-xl font-bold">{value}</p>
                </div>
              ))}
            </div>

            <div className="max-h-[320px] space-y-2 overflow-auto rounded-[20px] border border-border/70 p-3">
              {officialValidationReport.items.map((item) => (
                <div
                  key={`${item.questionId}-${item.sourceQuestionNumber}`}
                  className="rounded-[16px] bg-muted/40 p-3 text-sm"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant={item.updated ? "success" : "warning"}>
                      {item.updated ? "Atualizada" : "Sem mudança"}
                    </Badge>
                    <Badge variant={item.newValidatedAgainstOfficialSource ? "success" : "warning"}>
                      {item.newValidatedAgainstOfficialSource ? "INEP validado" : "Pendente INEP"}
                    </Badge>
                    <span className="font-semibold">
                      #{item.sourceQuestionNumber ?? "-"} {item.title}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">
                    Score: {item.previousScore ?? "-"} {"->"} {item.newScore ?? "-"} | Validação:{" "}
                    {String(item.previousValidatedAgainstOfficialSource)} {"->"}{" "}
                    {String(item.newValidatedAgainstOfficialSource)}
                  </p>
                  {item.recoveredAssets > 0 ? (
                    <p className="mt-2 text-xs font-medium text-emerald-600">
                      {item.recoveredAssets} asset(s) recuperado(s) do PDF oficial do INEP.
                    </p>
                  ) : null}
                  <div className="mt-3 grid gap-2 text-xs text-muted-foreground md:grid-cols-2">
                    <p>Idioma da questão: {item.languageOptionDetected ?? "-"}</p>
                    <p>Idioma da página: {item.pageLanguageOptionDetected ?? "-"}</p>
                    <p>Score da página: {item.selectedPageScore ?? "-"}</p>
                    <p>Crop method: {item.cropMethod ?? "-"}</p>
                    <p>Múltiplas questões no crop: {item.cropContainsMultipleQuestions ? "sim" : "não"}</p>
                    <p>Revisão manual do asset: {item.assetNeedsManualReview ? "sim" : "não"}</p>
                  </div>
                  {item.strongPhraseMatches.length ? (
                    <p className="mt-2 break-words text-xs text-muted-foreground">
                      Frases fortes encontradas: {item.strongPhraseMatches.join(" | ")}
                    </p>
                  ) : null}
                  {item.rejectedCandidatePages.length ? (
                    <p className="mt-2 break-words text-xs text-amber-700 dark:text-amber-300">
                      Páginas rejeitadas: {item.rejectedCandidatePages.join(" | ")}
                    </p>
                  ) : null}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}

      <div className="grid items-start gap-6 min-[1380px]:grid-cols-[320px_minmax(640px,1fr)] min-[1580px]:grid-cols-[320px_minmax(720px,1fr)_340px]">
        <Card className="min-w-0 overflow-hidden xl:sticky xl:top-24">
          <CardHeader className="border-b border-border/70">
            <div className="flex items-center justify-between gap-3">
              <CardTitle>Fila de revisão</CardTitle>
              {reviewQuery.data ? (
                <Badge variant="outline">{reviewQuery.data.metadata.totalElements} itens</Badge>
              ) : null}
            </div>
          </CardHeader>
          <CardContent className="space-y-4 pt-6">
            {reviewQuery.isError ? (
              <ErrorState
                title="Não foi possível carregar a fila de revisão."
                description="Tente novamente para buscar o estado mais recente do backend."
                onRetry={() => void reviewQuery.refetch()}
              />
            ) : reviewItems.length ? (
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
                    Página {(reviewQuery.data?.metadata.page ?? 0) + 1} de{" "}
                    {Math.max(reviewQuery.data?.metadata.totalPages ?? 1, 1)}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={reviewQuery.data?.metadata.first}
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
                      disabled={reviewQuery.data?.metadata.last}
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
              </>
            ) : (
              <EmptyState
                title="Nenhuma questão encontrada"
                description="Ajuste os filtros para localizar itens em revisão, validação ou auditoria."
              />
            )}
          </CardContent>
        </Card>

        <div className="min-w-0 space-y-6 min-[1380px]:min-w-[640px]">
          {selectedId === null ? (
            <EmptyState
              title="Selecione uma questão"
              description="Escolha um item da fila para revisar o enunciado, os assets e as informações de auditoria."
            />
          ) : detailQuery.isError ? (
            <ErrorState
              title="Não foi possível carregar o detalhe da revisão."
              description="Tente novamente para abrir a questão selecionada."
              onRetry={() => void detailQuery.refetch()}
            />
          ) : selectedQuestion ? (
            <Card className="min-w-0 overflow-hidden">
              <CardHeader className="border-b border-border/70 bg-gradient-to-r from-background via-background to-primary/5">
                <div className="space-y-5">
                  <div className="flex flex-col gap-4 2xl:flex-row 2xl:items-start 2xl:justify-between">
                    <div className="min-w-0 space-y-3">
                      <div className="flex flex-wrap gap-2">
                        <Badge variant={statusBadgeVariant(selectedQuestion.importStatus)}>
                          {selectedQuestion.importStatus}
                        </Badge>
                        <Badge variant="outline">{selectedQuestion.source}</Badge>
                        <Badge variant="outline">{selectedQuestion.subject}</Badge>
                        <Badge variant="outline">{selectedQuestion.difficulty}</Badge>
                        <Badge
                          variant={
                            selectedQuestion.validatedAgainstOfficialSource ? "success" : "warning"
                          }
                        >
                          {selectedQuestion.validatedAgainstOfficialSource
                            ? "Fonte oficial validada"
                            : "Pendente INEP"}
                        </Badge>
                      </div>
                      <CardTitle className="break-normal text-2xl [overflow-wrap:anywhere]">
                        {selectedQuestion.title}
                      </CardTitle>
                      <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2 xl:grid-cols-3">
                        <p>Ano: {selectedQuestion.sourceYear ?? "-"}</p>
                        <p>Questão: {selectedQuestion.sourceQuestionNumber ?? "-"}</p>
                        <p>Caderno: {selectedQuestion.sourceBookColor ?? "-"}</p>
                        <p>Dia: {selectedQuestion.sourceDay ?? "-"}</p>
                        <p>Batch: {selectedQuestion.importBatchId ?? "-"}</p>
                        <p>Assets: {selectedQuestion.assetsCount}</p>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-2 2xl:justify-end">
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
                        Próxima
                        <ArrowRight className="size-4" />
                      </Button>
                    </div>
                  </div>

                  <div className="grid gap-2 sm:grid-cols-2 2xl:grid-cols-4">
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
                      Manter em revisão
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => void handleStatusAndContinue("INVALID")}
                      disabled={updateStatus.isPending}
                    >
                      Marcar inválida
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
                      variant="outline"
                      onClick={() => void handleStatusAndContinue("NEEDS_REVIEW")}
                      disabled={updateStatus.isPending}
                    >
                      Salvar e próxima
                    </Button>
                    <Button
                      onClick={() => {
                        if (!window.confirm("Publicar esta questão para os alunos?")) {
                          return;
                        }
                        publishQuestion.mutate(selectedQuestion.id);
                      }}
                      disabled={!canPublish(selectedQuestion) || publishQuestion.isPending}
                    >
                      Publicar
                    </Button>
                  </div>

                  {publishBlockers.length ? (
                    <div className="rounded-[22px] border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-800 dark:text-amber-200">
                      <p className="font-semibold">Publicação bloqueada</p>
                      <p className="mt-1">
                        A questão ainda não pode ser publicada por estes motivos:
                      </p>
                      <div className="mt-3 flex flex-wrap gap-2">
                        {publishBlockers.map((reason) => (
                          <Badge key={reason} variant="warning">
                            {reason}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-start gap-3 rounded-[22px] border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
                      <p>Questão elegível para publicação quando você confirmar a decisão editorial.</p>
                    </div>
                  )}
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
                    <p>Nenhum alerta automático encontrado nesta revisão.</p>
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

                <Card className="min-w-0 overflow-hidden border-border/70 bg-background/60">
                  <CardHeader>
                    <CardTitle>Detalhe da questão</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex flex-wrap gap-2">
                      <Badge variant="outline">
                        Gabarito: {selectedQuestion.correctAlternative ?? "-"}
                      </Badge>
                      <Badge variant="outline">
                        Alternativas: {selectedQuestion.alternativesCount}
                      </Badge>
                      <Badge variant={autoStatusBadgeVariant(selectedQuestion.autoValidationStatus)}>
                        Auto score {selectedQuestion.autoValidationScore}
                      </Badge>
                      <Badge variant="outline">{selectedQuestion.autoValidationStatus}</Badge>
                      {selectedQuestion.requiresAssetReview ? (
                        <Badge variant="warning">Asset recovery needs review</Badge>
                      ) : null}
                    </div>

                    <div className="space-y-4 rounded-[22px] border border-border/70 bg-background/70 p-4">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="text-sm font-semibold">Editar enunciado</p>
                        {editForm.statement?.includes("ТЕХТО") ? (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              setEditForm((current) => ({
                                ...current,
                                statement: normalizeSuspiciousStatement(current.statement ?? ""),
                              }))
                            }
                          >
                            Corrigir TEXTO II
                          </Button>
                        ) : null}
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="review-statement">Enunciado</Label>
                        <Textarea
                          id="review-statement"
                          value={editForm.statement ?? ""}
                          onChange={(event) =>
                            setEditForm((current) => ({ ...current, statement: event.target.value }))
                          }
                          className="min-h-40 leading-6"
                        />
                      </div>
                      <div className="grid gap-3 md:grid-cols-3">
                        <div className="space-y-2">
                          <Label htmlFor="review-topic">Topic</Label>
                          <Input
                            id="review-topic"
                            value={editForm.topic ?? ""}
                            onChange={(event) =>
                              setEditForm((current) => ({ ...current, topic: event.target.value }))
                            }
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="review-subtopic">Subtopic</Label>
                          <Input
                            id="review-subtopic"
                            value={editForm.subtopic ?? ""}
                            onChange={(event) =>
                              setEditForm((current) => ({ ...current, subtopic: event.target.value }))
                            }
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="review-difficulty">Dificuldade</Label>
                          <select
                            id="review-difficulty"
                            className="flex h-11 w-full rounded-2xl border border-border bg-background/70 px-4 py-2 text-sm"
                            value={editForm.difficulty ?? "MEDIUM"}
                            onChange={(event) =>
                              setEditForm((current) => ({
                                ...current,
                                difficulty: event.target.value as DifficultyLevel,
                              }))
                            }
                          >
                            <option value="EASY">EASY</option>
                            <option value="MEDIUM">MEDIUM</option>
                            <option value="HARD">HARD</option>
                          </select>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Button
                          variant="secondary"
                          onClick={() =>
                            updateReviewQuestion.mutate({
                              id: selectedQuestion.id,
                              payload: {
                                statement: editForm.statement,
                                topic: editForm.topic,
                                subtopic: editForm.subtopic ?? "",
                                difficulty: editForm.difficulty,
                              },
                            })
                          }
                          disabled={updateReviewQuestion.isPending}
                        >
                          Salvar enunciado
                        </Button>
                      </div>
                    </div>

                    <QuestionContent
                      statement={selectedQuestion.statement}
                      statementHtml={renderableStatementHtml}
                      assets={selectedQuestion.assets}
                      sourceLabel="Assets vinculados"
                      requiresAssetReview={selectedQuestion.requiresAssetReview}
                      onRemoveAsset={(asset) => {
                        if (!window.confirm("Remover este asset da questão em revisão?")) {
                          return;
                        }
                        removeReviewAsset.mutate({
                          questionId: selectedQuestion.id,
                          assetId: asset.id,
                        });
                      }}
                    />

                    {selectedAsset ? (
                      <div className="space-y-4 rounded-[22px] border border-border/70 bg-background/70 p-4">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <p className="text-sm font-semibold">Revisão manual do asset</p>
                          {selectedQuestion.requiresAssetReview ? (
                            <Badge variant="warning">Revisão manual pendente</Badge>
                          ) : (
                            <Badge variant="success">Asset aprovado</Badge>
                          )}
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="selected-asset">Asset</Label>
                          <select
                            id="selected-asset"
                            className="flex h-11 w-full rounded-2xl border border-border bg-background/70 px-4 py-2 text-sm"
                            value={selectedAsset.id}
                            onChange={(event) => setSelectedAssetId(Number(event.target.value))}
                          >
                            {selectedQuestion.assets.map((asset) => (
                              <option key={asset.id} value={asset.id}>
                                Asset #{asset.id} - página {asset.sourcePage ?? "-"}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="grid gap-3 md:grid-cols-4">
                          <div className="space-y-2">
                            <Label htmlFor="crop-x">cropX</Label>
                            <Input
                              id="crop-x"
                              type="number"
                              value={cropForm.cropX}
                              onChange={(event) =>
                                setCropForm((current) => ({
                                  ...current,
                                  cropX: Number(event.target.value || 0),
                                }))
                              }
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="crop-y">cropY</Label>
                            <Input
                              id="crop-y"
                              type="number"
                              value={cropForm.cropY}
                              onChange={(event) =>
                                setCropForm((current) => ({
                                  ...current,
                                  cropY: Number(event.target.value || 0),
                                }))
                              }
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="crop-width">cropWidth</Label>
                            <Input
                              id="crop-width"
                              type="number"
                              value={cropForm.cropWidth}
                              onChange={(event) =>
                                setCropForm((current) => ({
                                  ...current,
                                  cropWidth: Number(event.target.value || 1),
                                }))
                              }
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="crop-height">cropHeight</Label>
                            <Input
                              id="crop-height"
                              type="number"
                              value={cropForm.cropHeight}
                              onChange={(event) =>
                                setCropForm((current) => ({
                                  ...current,
                                  cropHeight: Number(event.target.value || 1),
                                }))
                              }
                            />
                          </div>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="outline"
                            onClick={() =>
                              updateReviewAssetCrop.mutate({
                                questionId: selectedQuestion.id,
                                assetId: selectedAsset.id,
                                payload: cropForm,
                              })
                            }
                            disabled={updateReviewAssetCrop.isPending}
                          >
                            Ajustar crop
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() =>
                              approveReviewAsset.mutate({
                                questionId: selectedQuestion.id,
                                assetId: selectedAsset.id,
                              })
                            }
                            disabled={approveReviewAsset.isPending}
                          >
                            Aprovar asset
                          </Button>
                        </div>
                      </div>
                    ) : null}

                    <QuestionRawText value={selectedQuestion.statement} />
                  </CardContent>
                </Card>

                <Card className="min-w-0 overflow-hidden border-border/70 bg-background/60">
                  <CardHeader>
                    <CardTitle>Alternativas</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {selectedQuestion.alternatives.map((alternative) => (
                      <div
                        key={alternative.id}
                        className="rounded-[22px] border border-border/70 bg-background/70 p-4"
                      >
                        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                          <p className="text-sm font-semibold">Alternativa {alternative.letter}</p>
                          {selectedQuestion.correctAlternative === alternative.letter ? (
                            <Badge variant="success">Gabarito</Badge>
                          ) : null}
                        </div>
                        <AlternativeContent alternative={alternative} />
                      </div>
                    ))}

                    {selectedQuestion.explanation ? (
                      <div className="rounded-[22px] border border-border/70 bg-background/70 p-4">
                        <p className="text-sm font-semibold">Explicação</p>
                        <p className="mt-2 text-sm leading-7 text-muted-foreground">
                          {selectedQuestion.explanation}
                        </p>
                      </div>
                    ) : null}
                  </CardContent>
                </Card>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardContent className="p-6">Carregando detalhe...</CardContent>
            </Card>
          )}
        </div>

        <div className="min-w-0 space-y-6 min-[1380px]:col-span-2 min-[1580px]:col-span-1 min-[1580px]:sticky min-[1580px]:top-24">
          {selectedQuestion ? (
            <>
              <Card className="min-w-0 overflow-hidden border-border/70 bg-background/60">
                <CardHeader>
                  <CardTitle>Auditoria e origem</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4 text-sm">
                  <div className="space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                      Origem
                    </p>
                    <AuditField label="source" value={selectedQuestion.source} compact />
                    <AuditField
                      label="externalProvider"
                      value={selectedQuestion.externalProvider}
                      compact
                    />
                    <AuditField label="sourceBookColor" value={selectedQuestion.sourceBookColor} compact />
                    <AuditField label="sourceDay" value={selectedQuestion.sourceDay} compact />
                    <AuditField label="sourcePage" value={selectedQuestion.sourcePage} compact />
                    <AuditField label="importBatchId" value={selectedQuestion.importBatchId} compact />
                    <AuditUrlField label="sourceUrl" value={selectedQuestion.sourceUrl} />
                  </div>

                  <div className="space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                      Fonte oficial
                    </p>
                    <AuditField
                      label="validatedAgainstOfficialSource"
                      value={formatBoolean(selectedQuestion.validatedAgainstOfficialSource)}
                      compact
                    />
                    <AuditField label="validatedAt" value={selectedQuestion.validatedAt} compact />
                    <AuditField label="officialPage" value={selectedQuestion.officialPage} compact />
                    <AuditUrlField label="officialSourceUrl" value={selectedQuestion.officialSourceUrl} />
                    <AuditUrlField label="officialPdfUrl" value={selectedQuestion.officialPdfUrl} />
                    <AuditUrlField
                      label="officialAnswerKeyUrl"
                      value={selectedQuestion.officialAnswerKeyUrl}
                    />
                  </div>

                  <div className="space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                      Checks
                    </p>
                    <AuditField label="statementHash" value={selectedQuestion.statementHash} />
                    <AuditField label="assetsCount" value={selectedQuestion.assetsCount} compact />
                    <AuditField
                      label="alternativesCount"
                      value={selectedQuestion.alternativesCount}
                      compact
                    />
                    <AuditField
                      label="autoValidationScore"
                      value={selectedQuestion.autoValidationScore}
                      compact
                    />
                    <AuditField
                      label="autoValidationStatus"
                      value={selectedQuestion.autoValidationStatus}
                      compact
                    />
                    <AuditField label="autoValidatedAt" value={selectedQuestion.autoValidatedAt} compact />
                  </div>

                  {(autoWarnings.length || autoErrors.length) ? (
                    <div className="space-y-3">
                      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                        Auto validação
                      </p>
                      {autoErrors.length ? (
                        <div className="rounded-[18px] border border-rose-500/30 bg-rose-500/10 p-3">
                          <p className="text-sm font-semibold text-rose-600">Errors</p>
                          <div className="mt-2 space-y-2">
                            {autoErrors.map((item) => (
                              <p key={item} className="break-words text-sm text-muted-foreground">
                                {item}
                              </p>
                            ))}
                          </div>
                        </div>
                      ) : null}
                      {autoWarnings.length ? (
                        <div className="rounded-[18px] border border-amber-500/30 bg-amber-500/10 p-3">
                          <p className="text-sm font-semibold text-amber-700 dark:text-amber-300">
                            Warnings
                          </p>
                          <div className="mt-2 space-y-2">
                            {autoWarnings.map((item) => (
                              <p key={item} className="break-words text-sm text-muted-foreground">
                                {item}
                              </p>
                            ))}
                          </div>
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                </CardContent>
              </Card>

              <Card className="min-w-0 overflow-hidden border-border/70 bg-background/60">
                <CardHeader>
                  <CardTitle>Validação manual com INEP</CardTitle>
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
                    <Label htmlFor="officialPage">Página oficial</Label>
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
            </>
          ) : null}
        </div>
      </div>

      <Card className="overflow-hidden">
        <CardHeader className="border-b border-border/70">
          <CardTitle>Fontes oficiais INEP cadastradas</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-6 pt-6 xl:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
          <div className="space-y-3">
            {officialSourcesQuery.data?.length ? (
              officialSourcesQuery.data.slice(0, 5).map((source) => (
                <div
                  key={source.id}
                  className="rounded-[20px] border border-border/70 bg-background/70 p-4 text-sm"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="outline">
                      {source.exam} {source.year}
                    </Badge>
                    <Badge variant="outline">Dia {source.day ?? "-"}</Badge>
                    <Badge variant="outline">{source.bookColor || "cor livre"}</Badge>
                    {source.answerKeyMapJson ? (
                      <Badge variant="success">Gabarito estruturado</Badge>
                    ) : (
                      <Badge variant="warning">Sem mapa de gabarito</Badge>
                    )}
                  </div>
                  <p className="mt-3 break-words text-muted-foreground">PDF: {source.pdfUrl}</p>
                  <p className="mt-1 break-words text-muted-foreground">
                    PDF cacheado: {source.cachedPdfUrl ?? source.localPdfPath ?? "-"}
                  </p>
                  <p className="mt-1 break-words text-muted-foreground">
                    Gabarito: {source.answerKeyUrl ?? "-"}
                  </p>
                  <p className="mt-1 break-words text-muted-foreground">
                    Gabarito cacheado: {source.cachedAnswerKeyUrl ?? "-"}
                  </p>
                  <p className="mt-2 text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                    Recuperação de assets: {source.cachedPdfUrl ? "usa cache se o INEP falhar" : "usa URL oficial"}
                  </p>
                  {source.cachedPdfUrl ? (
                    <Button
                      className="mt-3 mr-2"
                      variant="outline"
                      onClick={() => void copyToClipboard(source.cachedPdfUrl)}
                    >
                      Copiar PDF cacheado
                    </Button>
                  ) : null}
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
                <Input
                  value={officialSourceForm.exam}
                  onChange={(event) =>
                    setOfficialSourceForm((current) => ({ ...current, exam: event.target.value }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label>Ano</Label>
                <Input
                  type="number"
                  value={officialSourceForm.year}
                  onChange={(event) =>
                    setOfficialSourceForm((current) => ({
                      ...current,
                      year: Number(event.target.value),
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label>Dia</Label>
                <Input
                  type="number"
                  value={officialSourceForm.day ?? ""}
                  onChange={(event) =>
                    setOfficialSourceForm((current) => ({
                      ...current,
                      day: event.target.value ? Number(event.target.value) : null,
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label>Caderno/cor</Label>
                <Input
                  value={officialSourceForm.bookColor ?? ""}
                  onChange={(event) =>
                    setOfficialSourceForm((current) => ({
                      ...current,
                      bookColor: event.target.value,
                    }))
                  }
                  placeholder="AZUL"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>pdfUrl oficial</Label>
              <Input
                value={officialSourceForm.pdfUrl}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({ ...current, pdfUrl: event.target.value }))
                }
                placeholder="https://www.gov.br/inep/..."
              />
            </div>
            <div className="space-y-2">
              <Label>answerKeyUrl oficial</Label>
              <Input
                value={officialSourceForm.answerKeyUrl ?? ""}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({
                    ...current,
                    answerKeyUrl: event.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>sourceUrl oficial</Label>
              <Input
                value={officialSourceForm.sourceUrl}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({
                    ...current,
                    sourceUrl: event.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>cachedPdfUrl opcional</Label>
              <Input
                value={officialSourceForm.cachedPdfUrl ?? ""}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({
                    ...current,
                    cachedPdfUrl: event.target.value,
                  }))
                }
                placeholder="https://.../official-exam-pdfs/enem/2023/day-1/azul.pdf"
              />
              <p className="text-xs text-muted-foreground">
                Use apenas cópia do PDF oficial do INEP em storage controlado.
              </p>
            </div>
            <div className="space-y-2">
              <Label>cachedAnswerKeyUrl opcional</Label>
              <Input
                value={officialSourceForm.cachedAnswerKeyUrl ?? ""}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({
                    ...current,
                    cachedAnswerKeyUrl: event.target.value,
                  }))
                }
                placeholder="https://.../official-exam-pdfs/enem/2023/dia-1/azul-gabarito.pdf"
              />
            </div>
            <div className="space-y-2">
              <Label>Mapa de gabarito JSON opcional</Label>
              <Textarea
                value={officialSourceForm.answerKeyMapJson ?? ""}
                onChange={(event) =>
                  setOfficialSourceForm((current) => ({
                    ...current,
                    answerKeyMapJson: event.target.value,
                  }))
                }
                placeholder='{"2":"A","3":"B"}'
              />
            </div>
            <Button
              className="w-full"
              onClick={() => void handleCreateOfficialSource()}
              disabled={createOfficialSource.isPending}
            >
              Cadastrar fonte oficial
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
