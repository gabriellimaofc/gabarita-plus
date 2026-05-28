import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const VISUAL_HINTS = [
  "observe o grafico",
  "a figura mostra",
  "na imagem",
  "o mapa",
  "a tabela",
  "o esquema",
  "o desenho",
  "a charge",
  "o infografico",
  "a tirinha",
  "o diagrama",
  "a seguir",
  "a imagem",
  "a ilustracao",
];

const BROKEN_MARKERS = ["Ã§", "Ã£", "Ã©", "Ãª", "Ã³", "Ãº", "â€œ", "â€\u009d", "â€“"];

export function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

export function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

export function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

export function generateStatementHash(question) {
  const base = normalizeText(question.statementHtml || question.statement || "");
  return crypto.createHash("sha256").update(base, "utf8").digest("hex");
}

export function normalizeAlternatives(question) {
  const alternatives = Array.isArray(question.alternatives) ? [...question.alternatives] : [];
  const normalized = alternatives
    .map((alternative) => ({
      ...alternative,
      letter: String(alternative.letter || "").trim().toUpperCase(),
      assets: Array.isArray(alternative.assets) ? alternative.assets : [],
    }))
    .sort((left, right) => left.letter.localeCompare(right.letter));

  return {
    ...question,
    correctAlternative: String(question.correctAlternative || "").trim().toUpperCase(),
    alternatives: normalized,
  };
}

export function detectNeedsReview(question) {
  const normalized = normalizeText(`${question.statement || ""} ${question.statementHtml || ""}`);
  const hasVisualHint = VISUAL_HINTS.some((hint) => normalized.includes(hint));
  const hasAssets = Array.isArray(question.assets) && question.assets.length > 0;
  const hasBrokenText = BROKEN_MARKERS.some((marker) =>
    `${question.statement || ""} ${question.statementHtml || ""}`.includes(marker),
  );
  return {
    hasVisualHint,
    hasAssets,
    hasBrokenText,
    needsReview: hasBrokenText || (hasVisualHint && !hasAssets),
  };
}

export function validateQuestion(question) {
  const errors = [];
  const normalized = normalizeAlternatives(question);
  const letters = normalized.alternatives.map((alternative) => alternative.letter);

  if (!normalized.statement?.trim()) errors.push("statement obrigatorio");
  if (!normalized.subject?.trim()) errors.push("subject obrigatorio");
  if (!normalized.exam?.trim()) errors.push("exam obrigatorio");
  if (!normalized.source?.trim()) errors.push("source obrigatorio");
  if (!normalized.sourceUrl?.trim()) errors.push("sourceUrl obrigatorio");
  if (!normalized.year) errors.push("year obrigatorio");
  if (!normalized.sourceQuestionNumber) errors.push("sourceQuestionNumber obrigatorio");
  if (normalized.alternatives.length !== 5) errors.push("devem existir exatamente 5 alternativas");
  if (letters.join(",") !== "A,B,C,D,E") errors.push("alternativas devem ser A-E");
  if (!["A", "B", "C", "D", "E"].includes(normalized.correctAlternative)) {
    errors.push("gabarito deve estar entre A-E");
  }

  const review = detectNeedsReview(normalized);
  return {
    normalized,
    errors,
    review,
    statementHash: generateStatementHash(normalized),
  };
}

export function findDuplicates(questions) {
  const seen = new Map();
  const duplicates = [];

  questions.forEach((question, index) => {
    const hash = generateStatementHash(question);
    const key = [
      question.sourceExam || question.exam || "",
      question.sourceYear || question.year || "",
      question.sourceQuestionNumber || "",
      question.sourceDay || "",
      question.sourceBookColor || "",
      hash,
    ].join("|");

    if (seen.has(key)) {
      duplicates.push({ index, duplicateOf: seen.get(key), key, title: question.title });
      return;
    }

    seen.set(key, index);
  });

  return duplicates;
}

export function validateAssets(questions, baseDir = process.cwd()) {
  return questions.map((question, index) => {
    const missing = [];
    for (const asset of question.assets || []) {
      if (asset.storagePath) {
        const resolved = path.resolve(baseDir, asset.storagePath);
        if (!fs.existsSync(resolved)) {
          missing.push(asset.storagePath);
        }
      }
    }
    return { index, title: question.title, missing };
  });
}

export function summarizeQuestions(questions) {
  const duplicates = findDuplicates(questions);
  const review = questions.filter((question) => detectNeedsReview(question).needsReview).length;
  const invalid = questions.filter((question) => validateQuestion(question).errors.length > 0).length;

  return {
    total: questions.length,
    duplicates: duplicates.length,
    needsReview: review,
    invalid,
  };
}
