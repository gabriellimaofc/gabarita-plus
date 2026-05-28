import { loadJson, validateQuestion } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/validate-json.mjs <arquivo.json>");

const questions = loadJson(inputFile);
const report = questions.map((question, index) => ({ index, ...validateQuestion(question) }));
console.log(JSON.stringify(report, null, 2));
