import {
  getRawFilePath,
  normalizeQuestionSet,
  parseCliArgs,
  saveNormalizedPayload,
} from "./lib/enem-dev-utils.mjs";
import fs from "node:fs";

const options = parseCliArgs(process.argv.slice(2));
if (!options.year) {
  console.error("Uso: node tools/import-enem/enem-dev/normalize-enem-dev.mjs <ano>");
  process.exit(1);
}

const rawFile = getRawFilePath(options.year);
if (!fs.existsSync(rawFile)) {
  console.error(`Arquivo bruto nao encontrado: ${rawFile}`);
  process.exit(1);
}

const rawPayload = JSON.parse(fs.readFileSync(rawFile, "utf8"));
const normalized = normalizeQuestionSet(rawPayload);
saveNormalizedPayload(options.year, normalized);

console.log(`Ano: ${options.year}`);
console.log(`Questoes normalizadas: ${normalized.length}`);
console.log(`Arquivo: database/imports/enem-dev/${options.year}/questions.normalized.json`);
