import { loadJson, normalizeAlternatives, writeJson } from "./lib/enem-import-utils.mjs";

const [inputFile, outputFile = inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/normalize-alternatives.mjs <in.json> [out.json]");

const normalized = loadJson(inputFile).map(normalizeAlternatives);
writeJson(outputFile, normalized);
console.log(`Alternativas normalizadas em ${outputFile}`);
