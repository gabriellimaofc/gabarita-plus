import {
  buildReviewReport,
  loadNormalizedPayload,
  parseCliArgs,
  saveReviewReport,
} from "./lib/enem-dev-utils.mjs";

const options = parseCliArgs(process.argv.slice(2));
if (!options.year) {
  console.error("Uso: node tools/import-enem/enem-dev/generate-review-report.mjs <ano>");
  process.exit(1);
}

const normalized = loadNormalizedPayload(options.year);
const markdown = buildReviewReport(options.year, normalized);
saveReviewReport(options.year, markdown);

console.log(`Ano: ${options.year}`);
console.log(`Arquivo: database/imports/enem-dev/${options.year}/review-report.md`);
