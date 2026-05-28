import { loadJson, validateAssets } from "./lib/enem-import-utils.mjs";

const [inputFile, baseDir = process.cwd()] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/validate-assets.mjs <arquivo.json> [baseDir]");

console.log(JSON.stringify(validateAssets(loadJson(inputFile), baseDir), null, 2));
