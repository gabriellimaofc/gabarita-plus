import { generateStatementHash, loadJson } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/generate-statement-hash.mjs <arquivo.json>");

const questions = loadJson(inputFile);
console.log(
  JSON.stringify(
    questions.map((question, index) => ({
      index,
      title: question.title,
      statementHash: generateStatementHash(question),
    })),
    null,
    2,
  ),
);
