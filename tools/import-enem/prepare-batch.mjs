import { loadJson, writeJson } from "./lib/enem-import-utils.mjs";

const [inputFile, year, day, bookColor, outputFile = `enem-${year}-dia-${day}-${bookColor}.json`] = process.argv.slice(2);
if (!inputFile || !year || !day || !bookColor) {
  throw new Error("Uso: node tools/import-enem/prepare-batch.mjs <arquivo.json> <ano> <dia> <caderno> [saida.json]");
}

const filtered = loadJson(inputFile).filter((question) => {
  const sourceYear = Number(question.sourceYear || question.year);
  const sourceDay = Number(question.sourceDay || 0);
  const sourceBookColor = String(question.sourceBookColor || "").toUpperCase();
  return sourceYear === Number(year) && sourceDay === Number(day) && sourceBookColor === String(bookColor).toUpperCase();
});

writeJson(outputFile, filtered);
console.log(`Lote ${year}/dia-${day}/${bookColor} preparado em ${outputFile}`);
