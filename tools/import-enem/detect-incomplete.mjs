import { detectNeedsReview, loadJson } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/detect-incomplete.mjs <arquivo.json>");

console.log(
  JSON.stringify(
    loadJson(inputFile).map((question, index) => ({
      index,
      title: question.title,
      ...detectNeedsReview(question),
    })),
    null,
    2,
  ),
);
