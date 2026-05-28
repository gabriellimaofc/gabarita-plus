import {
  authenticateAdmin,
  loadNormalizedPayload,
  parseCliArgs,
  saveDryRunPayload,
  stripReviewMetadata,
} from "./lib/enem-dev-utils.mjs";

const options = parseCliArgs(process.argv.slice(2));
if (!options.year) {
  console.error("Uso: node tools/import-enem/enem-dev/dry-run-enem-dev.mjs <ano> [--api-base-url=...]");
  process.exit(1);
}

const normalized = loadNormalizedPayload(options.year).map(stripReviewMetadata);
const token = await authenticateAdmin(options.apiBaseUrl, options.adminEmail, options.adminPassword);

const response = await fetch(`${options.apiBaseUrl}/admin/import/enem-dev/dry-run`, {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
    Authorization: `Bearer ${token}`,
  },
  body: JSON.stringify({
    year: options.year,
    limit: options.limit,
    offset: options.offset,
    language: options.language,
  }),
});

if (!response.ok) {
  const body = await response.text();
  throw new Error(`Dry-run falhou: ${response.status} ${response.statusText} :: ${body.slice(0, 400)}`);
}

const payload = await response.json();
saveDryRunPayload(options.year, payload);

const report = payload?.data;
console.log(`Ano: ${options.year}`);
console.log(`API: ${options.apiBaseUrl}`);
console.log(`Admin: ${options.adminEmail}`);
console.log(`Token: ${token.slice(0, 10)}...${token.slice(-6)}`);
console.log(`Questoes locais normalizadas: ${normalized.length}`);
console.log(`Total processado: ${report?.totalProcessed ?? 0}`);
console.log(`Importadas previstas: ${(report?.totalProcessed ?? 0) - (report?.skippedDuplicates ?? 0) - (report?.invalid ?? 0)}`);
console.log(`Duplicadas previstas: ${report?.skippedDuplicates ?? 0}`);
console.log(`Needs review: ${report?.needsReview ?? 0}`);
console.log(`Invalidas: ${report?.invalid ?? 0}`);
console.log(`BatchId: ${String(report?.batchId)}`);
console.log(`DryRun: ${String(report?.dryRun)}`);
console.log(`Arquivo: database/imports/enem-dev/${options.year}/dry-run-report.json`);
