import { fetchQuestionsByYear, parseCliArgs, saveRawPayload } from "./lib/enem-dev-utils.mjs";

const options = parseCliArgs(process.argv.slice(2));
if (!options.year) {
  console.error("Uso: node tools/import-enem/enem-dev/fetch-year.mjs <ano> [--language=espanhol] [--limit=50]");
  process.exit(1);
}

const payload = await fetchQuestionsByYear(options.year, {
  language: options.language,
  limit: options.limit || 50,
  offset: options.offset || 0,
});

saveRawPayload(options.year, payload);

console.log(`Ano: ${options.year}`);
console.log(`Idioma: ${options.language || "default"}`);
console.log(`Questoes baixadas: ${payload.total}`);
console.log(`Arquivo: database/imports/enem-dev/${options.year}/questions.raw.json`);
