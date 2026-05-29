#!/usr/bin/env node

import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

const DEFAULT_MANIFEST = "tools/official-pdfs/enem-official-pdfs.manifest.json";
const USER_AGENT = "Mozilla/5.0 (compatible; GabaritaPlusOfficialPdfCache/1.0; +https://gabarita-plus.vercel.app)";

const manifestPath = resolve(process.argv[2] ?? DEFAULT_MANIFEST);
const apiBaseUrl = trimTrailingSlash(process.env.API_BASE_URL ?? "https://gabarita-plus-api.onrender.com/api");
const adminEmail = process.env.ADMIN_EMAIL;
const adminPassword = process.env.ADMIN_PASSWORD;
const supabaseUrl = trimTrailingSlash(process.env.SUPABASE_URL ?? "");
const supabaseServiceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
const bucket = process.env.SUPABASE_BUCKET_OFFICIAL_PDFS ?? "official-exam-pdfs";
const urlMode = (process.env.SUPABASE_PDF_URL_MODE ?? "public").toLowerCase();
const signedUrlExpiresSeconds = Number(process.env.SUPABASE_SIGNED_URL_EXPIRES_SECONDS ?? 60 * 60 * 24 * 365);
const reportPath = resolve(process.env.REPORT_PATH ?? "database/imports/official-pdfs/cache-report.json");

assertRequiredEnv("ADMIN_EMAIL", adminEmail);
assertRequiredEnv("ADMIN_PASSWORD", adminPassword);
assertRequiredEnv("SUPABASE_URL", supabaseUrl);
assertRequiredEnv("SUPABASE_SERVICE_ROLE_KEY", supabaseServiceRoleKey);

const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
if (!Array.isArray(manifest)) {
  throw new Error("Manifest must be a JSON array.");
}

const report = {
  startedAt: new Date().toISOString(),
  manifestPath,
  bucket,
  urlMode,
  downloaded: 0,
  uploaded: 0,
  alreadyExisting: 0,
  failed: 0,
  items: [],
};

await ensureBucket();
const accessToken = await loginAdmin();

for (const entry of manifest) {
  const item = {
    exam: entry.exam,
    year: entry.year,
    day: entry.day,
    bookColor: entry.bookColor,
    storagePdfPath: entry.storagePdfPath,
    storageAnswerKeyPath: entry.storageAnswerKeyPath,
    cachedPdfUrl: null,
    cachedAnswerKeyUrl: null,
    downloaded: [],
    uploaded: [],
    alreadyExisting: [],
    errors: [],
  };

  try {
    validateEntry(entry);
    const pdfBytes = await downloadPdfWithRetry(entry.pdfUrl, "prova", item);
    item.cachedPdfUrl = await uploadPdf(entry.storagePdfPath, pdfBytes, item);

    if (entry.answerKeyUrl && entry.storageAnswerKeyPath) {
      const answerKeyBytes = await downloadPdfWithRetry(entry.answerKeyUrl, "gabarito", item);
      item.cachedAnswerKeyUrl = await uploadPdf(entry.storageAnswerKeyPath, answerKeyBytes, item);
    }

    await upsertOfficialSource(entry, item.cachedPdfUrl, item.cachedAnswerKeyUrl);
  } catch (error) {
    report.failed += 1;
    item.errors.push(safeError(error));
  }

  report.items.push(item);
}

report.finishedAt = new Date().toISOString();
await mkdir(dirname(reportPath), { recursive: true });
await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");

console.log("Official PDF cache finished.");
console.log(`Manifest items: ${manifest.length}`);
console.log(`Downloaded: ${report.downloaded}`);
console.log(`Uploaded: ${report.uploaded}`);
console.log(`Already existing: ${report.alreadyExisting}`);
console.log(`Failed: ${report.failed}`);
console.log(`Report: ${reportPath}`);

async function loginAdmin() {
  const response = await fetch(`${apiBaseUrl}/auth/login`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ usernameOrEmail: adminEmail, password: adminPassword }),
  });
  const body = await readJson(response);
  if (!response.ok || !body?.success || !body?.data?.accessToken) {
    throw new Error(`Admin login failed with HTTP ${response.status}: ${body?.message ?? "unknown error"}`);
  }
  console.log(`Admin login OK for ${adminEmail}. Token received but not printed.`);
  return body.data.accessToken;
}

async function upsertOfficialSource(entry, cachedPdfUrl, cachedAnswerKeyUrl) {
  const payload = {
    exam: entry.exam,
    year: entry.year,
    day: entry.day,
    bookColor: entry.bookColor,
    pdfUrl: entry.pdfUrl,
    answerKeyUrl: entry.answerKeyUrl ?? null,
    sourceUrl: entry.sourceUrl,
    localPdfPath: null,
    cachedPdfUrl,
    cachedAnswerKeyUrl,
    answerKeyMapJson: entry.answerKeyMapJson ?? null,
  };

  const response = await fetch(`${apiBaseUrl}/admin/import/official-sources`, {
    method: "POST",
    headers: {
      authorization: `Bearer ${accessToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(payload),
  });
  const body = await readJson(response);
  if (!response.ok || !body?.success) {
    throw new Error(`Official source upsert failed with HTTP ${response.status}: ${body?.message ?? "unknown error"}`);
  }
}

async function downloadPdfWithRetry(url, label, item) {
  let lastError;
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    try {
      const response = await fetch(url, {
        headers: {
          "user-agent": USER_AGENT,
          accept: "application/pdf,application/octet-stream;q=0.9,*/*;q=0.1",
          "cache-control": "no-cache",
        },
        redirect: "follow",
      });
      const contentType = response.headers.get("content-type");
      const contentLength = response.headers.get("content-length");
      const buffer = Buffer.from(await response.arrayBuffer());
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      validatePdfBytes(buffer, url);
      report.downloaded += 1;
      item.downloaded.push({ label, url, bytes: buffer.length, contentType, contentLength });
      return buffer;
    } catch (error) {
      lastError = error;
      await sleep(500 * attempt * attempt);
    }
  }
  throw new Error(`Failed to download ${label} PDF after retries: ${safeError(lastError)}`);
}

async function ensureBucket() {
  const getResponse = await supabaseFetch(`/storage/v1/bucket/${bucket}`, { method: "GET" });
  if (getResponse.ok) {
    return;
  }
  if (getResponse.status !== 404) {
    throw new Error(`Could not validate Supabase bucket ${bucket}: HTTP ${getResponse.status}`);
  }

  const createResponse = await supabaseFetch("/storage/v1/bucket", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      id: bucket,
      name: bucket,
      public: urlMode === "public",
      file_size_limit: 52428800,
      allowed_mime_types: ["application/pdf"],
    }),
  });
  if (!createResponse.ok && createResponse.status !== 409) {
    const body = await createResponse.text();
    throw new Error(`Could not create Supabase bucket ${bucket}: HTTP ${createResponse.status} ${body}`);
  }
}

async function uploadPdf(storagePath, content, item) {
  const existsResponse = await supabaseFetch(`/storage/v1/object/info/${bucket}/${storagePath}`, { method: "GET" });
  if (existsResponse.ok) {
    report.alreadyExisting += 1;
    item.alreadyExisting.push(storagePath);
  }

  const uploadResponse = await supabaseFetch(`/storage/v1/object/${bucket}/${storagePath}`, {
    method: "POST",
    headers: {
      "content-type": "application/pdf",
      "x-upsert": "true",
    },
    body: content,
  });
  if (!uploadResponse.ok) {
    const body = await uploadResponse.text();
    throw new Error(`Upload failed for ${storagePath}: HTTP ${uploadResponse.status} ${body}`);
  }

  report.uploaded += 1;
  item.uploaded.push(storagePath);
  return buildObjectUrl(storagePath);
}

async function buildObjectUrl(storagePath) {
  if (urlMode === "signed") {
    const response = await supabaseFetch(`/storage/v1/object/sign/${bucket}/${storagePath}`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ expiresIn: signedUrlExpiresSeconds }),
    });
    const body = await readJson(response);
    if (!response.ok || !body?.signedURL) {
      throw new Error(`Could not create signed URL for ${storagePath}: HTTP ${response.status}`);
    }
    return body.signedURL.startsWith("http")
      ? body.signedURL
      : `${supabaseUrl}/storage/v1${body.signedURL}`;
  }
  return `${supabaseUrl}/storage/v1/object/public/${bucket}/${storagePath}`;
}

function supabaseFetch(path, options) {
  return fetch(`${supabaseUrl}${path}`, {
    ...options,
    headers: {
      authorization: `Bearer ${supabaseServiceRoleKey}`,
      apikey: supabaseServiceRoleKey,
      ...(options.headers ?? {}),
    },
  });
}

function validateEntry(entry) {
  for (const field of ["exam", "year", "day", "bookColor", "pdfUrl", "storagePdfPath", "sourceUrl"]) {
    if (entry[field] === undefined || entry[field] === null || entry[field] === "") {
      throw new Error(`Manifest item missing required field: ${field}`);
    }
  }
}

function validatePdfBytes(buffer, url) {
  if (!buffer || buffer.length < 1024) {
    throw new Error(`Downloaded PDF is too small: ${url}`);
  }
  if (buffer.subarray(0, 4).toString("ascii") !== "%PDF") {
    throw new Error(`Downloaded file does not start with %PDF: ${url}`);
  }
}

async function readJson(response) {
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function assertRequiredEnv(name, value) {
  if (!value) {
    throw new Error(`${name} is required. Do not hardcode secrets; pass it via environment variable.`);
  }
}

function trimTrailingSlash(value) {
  return value.replace(/\/+$/, "");
}

function safeError(error) {
  const message = error instanceof Error ? error.message : String(error);
  return message.length > 500 ? `${message.slice(0, 500)}...` : message;
}

function sleep(ms) {
  return new Promise((resolveSleep) => setTimeout(resolveSleep, ms));
}
