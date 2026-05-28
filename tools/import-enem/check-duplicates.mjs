import { findDuplicates, loadJson } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/check-duplicates.mjs <arquivo.json>");

console.log(JSON.stringify(findDuplicates(loadJson(inputFile)), null, 2));
