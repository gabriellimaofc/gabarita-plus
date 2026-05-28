import fs from "node:fs";
import path from "node:path";

import {
  detectNeedsReview,
  generateStatementHash,
  loadJson,
  summarizeQuestions,
  validateQuestion,
  writeJson,
} from "../../lib/enem-import-utils.mjs";

export const ENEM_DEV_API_BASE_URL = "https://api.enem.dev/v1";
export const ENEM_DEV_PROVIDER_URL = "https://enem.dev";

export function parseCliArgs(argv) {
  const options = {
    year: null,
    language: null,
    limit: null,
    offset: 0,
    apiBaseUrl: process.env.API_BASE_URL || "http://localhost:8080/api",
    adminEmail: process.env.ADMIN_EMAIL || "",
    adminPassword: process.env.ADMIN_PASSWORD || "",
  };

  for (const arg of argv) {
    if (!arg.startsWith("--") && options.year === null) {
      options.year = Number.parseInt(arg, 10);
      continue;
    }

    if (arg.startsWith("--language=")) {
      options.language = arg.slice("--language=".length);
      continue;
    }
    if (arg.startsWith("--limit=")) {
      options.limit = Number.parseInt(arg.slice("--limit=".length), 10);
      continue;
    }
    if (arg.startsWith("--offset=")) {
      options.offset = Number.parseInt(arg.slice("--offset=".length), 10);
      continue;
    }
    if (arg.startsWith("--api-base-url=")) {
      options.apiBaseUrl = arg.slice("--api-base-url=".length);
      continue;
    }
    if (arg.startsWith("--admin-email=")) {
      options.adminEmail = arg.slice("--admin-email=".length);
      continue;
    }
    if (arg.startsWith("--admin-password=")) {
      options.adminPassword = arg.slice("--admin-password=".length);
    }
  }

  return options;
}

export async function fetchJson(url, attempt = 1) {
  const response = await fetch(url, {
    headers: {
      Accept: "application/json",
      "User-Agent": "GabaritaPlus-Import/1.0",
    },
  });

  if (response.status === 429 && attempt <= 5) {
    const retryAfterHeader = response.headers.get("retry-after");
    const retryAfterMs = retryAfterHeader ? Number.parseInt(retryAfterHeader, 10) * 1000 : attempt * 500;
    await sleep(Number.isFinite(retryAfterMs) ? retryAfterMs : attempt * 500);
    return fetchJson(url, attempt + 1);
  }

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Falha ao consultar ${url}: ${response.status} ${response.statusText} :: ${body.slice(0, 400)}`);
  }

  return response.json();
}

export async function fetchYears() {
  return fetchJson(`${ENEM_DEV_API_BASE_URL}/exams`);
}

export async function fetchQuestionsByYear(year, { language = null, limit = 50, offset = 0 } = {}) {
  const questions = [];
  let hasMore = true;
  let currentOffset = offset;

  while (hasMore) {
    const params = new URLSearchParams({
      limit: String(limit),
      offset: String(currentOffset),
    });
    if (language) {
      params.set("language", language);
    }

    const page = await fetchJson(`${ENEM_DEV_API_BASE_URL}/exams/${year}/questions?${params.toString()}`);
    const items = Array.isArray(page.questions) ? page.questions : [];
    if (items.length === 0) {
      break;
    }

    questions.push(...items);
    hasMore = Boolean(page.metadata?.hasMore);
    currentOffset += limit;
  }

  return {
    year,
    language,
    fetchedAt: new Date().toISOString(),
    total: questions.length,
    questions,
  };
}

export function normalizeEnemDevQuestion(question, requestedYear) {
  const discipline = optional(question.discipline);
  const language = optional(question.language);
  const alternatives = Array.isArray(question.alternatives) ? question.alternatives : [];
  const normalizedAlternatives = alternatives.map((alternative) => ({
    letter: String(alternative.letter || "").trim().toUpperCase(),
    text: optional(alternative.text) || "",
    html: null,
    assets: alternative.file
      ? [
          {
            type: "IMAGE",
            url: alternative.file,
            storagePath: null,
            originalFileName: safeFileName(alternative.file),
            sourcePage: null,
            cropX: null,
            cropY: null,
            cropWidth: null,
            cropHeight: null,
            altText: "Asset externo vinculado a alternativa na API enem.dev.",
            caption: null,
            checksum: null,
          },
        ]
      : [],
  }));

  const normalized = {
    title: optional(question.title) || `ENEM ${requestedYear} - Questao ${question.index}`,
    statement: combineStatement(question.context, question.alternativesIntroduction),
    statementHtml: null,
    imageUrl: Array.isArray(question.files) && question.files.length > 0 ? question.files[0] : null,
    subject: normalizeSubject(discipline),
    topic: "A classificar",
    subtopic: normalizeSubtopic(language, discipline),
    difficulty: "MEDIUM",
    year: requestedYear,
    exam: "ENEM",
    competency: null,
    ability: null,
    explanation: null,
    correctAlternative: resolveCorrectAlternative(question, normalizedAlternatives),
    source: "ENEM_DEV",
    sourceUrl: `${ENEM_DEV_API_BASE_URL}/exams/${requestedYear}/questions/${question.index}`,
    sourceExam: "ENEM",
    sourceYear: requestedYear,
    sourceQuestionNumber: question.index,
    sourceBookColor: "UNKNOWN",
    sourceDay: inferDay(discipline),
    sourcePage: null,
    officialSourceUrl: null,
    officialPdfUrl: null,
    officialAnswerKeyUrl: null,
    officialPage: null,
    validatedAgainstOfficialSource: false,
    validatedAt: null,
    externalProvider: "enem.dev",
    externalProviderUrl: ENEM_DEV_PROVIDER_URL,
    externalQuestionId: `${requestedYear}:${question.index}:${discipline || "unknown"}:${language || "default"}`,
    externalLicense: null,
    assets: (Array.isArray(question.files) ? question.files : []).map((file) => ({
      type: "IMAGE",
      url: file,
      storagePath: null,
      originalFileName: safeFileName(file),
      sourcePage: null,
      cropX: null,
      cropY: null,
      cropWidth: null,
      cropHeight: null,
      altText: "Asset externo referenciado pela API enem.dev.",
      caption: null,
      checksum: null,
    })),
    alternatives: normalizedAlternatives,
  };

  return {
    ...normalized,
    statementHash: generateStatementHash(normalized),
    review: detectNeedsReview(normalized),
    validation: validateQuestion(normalized),
  };
}

export function normalizeQuestionSet(rawPayload) {
  const questions = Array.isArray(rawPayload.questions) ? rawPayload.questions : [];
  return questions.map((question) => normalizeEnemDevQuestion(question, rawPayload.year));
}

export function getImportDir(year) {
  return path.resolve("database", "imports", "enem-dev", String(year));
}

export function getRawFilePath(year) {
  return path.join(getImportDir(year), "questions.raw.json");
}

export function getNormalizedFilePath(year) {
  return path.join(getImportDir(year), "questions.normalized.json");
}

export function getDryRunFilePath(year) {
  return path.join(getImportDir(year), "dry-run-report.json");
}

export function getReviewReportPath(year) {
  return path.join(getImportDir(year), "review-report.md");
}

export function ensureImportDir(year) {
  fs.mkdirSync(getImportDir(year), { recursive: true });
}

export function saveRawPayload(year, payload) {
  ensureImportDir(year);
  writeJson(getRawFilePath(year), payload);
}

export function saveNormalizedPayload(year, normalized) {
  ensureImportDir(year);
  writeJson(
    getNormalizedFilePath(year),
    normalized.map(({ review, validation, statementHash, ...question }) => ({
      ...question,
      statementHash,
      review,
      validationErrors: validation.errors,
    })),
  );
}

export function loadNormalizedPayload(year) {
  return loadJson(getNormalizedFilePath(year));
}

export function saveDryRunPayload(year, payload) {
  ensureImportDir(year);
  writeJson(getDryRunFilePath(year), payload);
}

export function saveReviewReport(year, markdown) {
  ensureImportDir(year);
  fs.writeFileSync(getReviewReportPath(year), `${markdown}\n`, "utf8");
}

export function buildReviewReport(year, normalizedQuestions) {
  const normalizedForValidation = normalizedQuestions.map(stripReviewMetadata);
  const summary = summarizeQuestions(normalizedForValidation);
  const items = normalizedQuestions.map((question, index) => {
    const warnings = [];
    if (question.review?.hasBrokenText) warnings.push("texto com possivel encoding quebrado");
    if (question.review?.hasVisualHint && !question.review?.hasAssets) warnings.push("referencia visual sem asset");
    if (question.sourceDay == null) warnings.push("sourceDay nao inferido");
    if (Array.isArray(question.validationErrors) && question.validationErrors.length > 0) {
      warnings.push(`invalida: ${question.validationErrors.join("; ")}`);
    }
    if (!warnings.length) warnings.push("sem alertas estruturais");
    return `| ${index} | ${question.sourceQuestionNumber ?? "-"} | ${question.title} | ${warnings.join(" / ")} |`;
  });

  return [
    `# ENEM Dev Review Report ${year}`,
    "",
    `- Total: ${summary.total}`,
    `- Duplicatas locais potenciais: ${summary.duplicates}`,
    `- NEEDS_REVIEW potenciais: ${summary.needsReview}`,
    `- Invalidas potenciais: ${summary.invalid}`,
    "",
    "| Index | Numero | Titulo | Alertas |",
    "| --- | --- | --- | --- |",
    ...items,
  ].join("\n");
}

export function stripReviewMetadata(question) {
  const clone = { ...question };
  delete clone.review;
  delete clone.validation;
  delete clone.validationErrors;
  delete clone.statementHash;
  return clone;
}

export async function authenticateAdmin(apiBaseUrl, adminEmail, adminPassword) {
  if (!adminEmail || !adminPassword) {
    throw new Error("ADMIN_EMAIL e ADMIN_PASSWORD sao obrigatorios para o dry-run.");
  }

  const response = await fetch(`${apiBaseUrl}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      usernameOrEmail: adminEmail,
      password: adminPassword,
    }),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Falha no login admin: ${response.status} ${response.statusText} :: ${body.slice(0, 400)}`);
  }

  const payload = await response.json();
  const accessToken = payload?.data?.accessToken;
  if (!accessToken) {
    throw new Error("Resposta de login sem accessToken.");
  }

  return accessToken;
}

function resolveCorrectAlternative(question, normalizedAlternatives) {
  if (question.correctAlternative) {
    return String(question.correctAlternative).trim().toUpperCase();
  }
  const correct = normalizedAlternatives.find((alternative, index) =>
    Boolean(question.alternatives?.[index]?.isCorrect),
  );
  return correct?.letter || null;
}

function combineStatement(context, alternativesIntroduction) {
  const parts = [optional(context), optional(alternativesIntroduction)].filter(Boolean);
  return parts.join("\n\n");
}

function normalizeSubject(discipline) {
  switch ((discipline || "").toLowerCase()) {
    case "ciencias-humanas":
      return "Ciencias Humanas";
    case "ciencias-natureza":
      return "Ciencias da Natureza";
    case "linguagens":
      return "Linguagens";
    case "matematica":
      return "Matematica";
    default:
      return "A classificar";
  }
}

function normalizeSubtopic(language, discipline) {
  if (language) {
    return language.charAt(0).toUpperCase() + language.slice(1).toLowerCase();
  }
  return discipline || "A classificar";
}

function inferDay(discipline) {
  switch ((discipline || "").toLowerCase()) {
    case "linguagens":
    case "ciencias-humanas":
      return 1;
    case "matematica":
    case "ciencias-natureza":
      return 2;
    default:
      return null;
  }
}

function safeFileName(url) {
  const file = String(url || "").split("/").pop();
  return file || null;
}

function optional(value) {
  if (value == null) {
    return null;
  }
  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
