import { loadJson, writeJson } from "./lib/enem-import-utils.mjs";

const [inputFile, year, outputFile = `enem-${year}.json`] = process.argv.slice(2);
if (!inputFile || !year) throw new Error("Uso: node tools/import-enem/prepare-year-import.mjs <arquivo.json> <ano> [saida.json]");

const filtered = loadJson(inputFile).filter((question) => Number(question.sourceYear || question.year) === Number(year));
writeJson(outputFile, filtered);
console.log(`Arquivo do ano ${year} gerado em ${outputFile}`);
