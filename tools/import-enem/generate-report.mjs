import { loadJson, summarizeQuestions } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/generate-report.mjs <arquivo.json>");

console.log(JSON.stringify(summarizeQuestions(loadJson(inputFile)), null, 2));
