import { loadJson } from "./lib/enem-import-utils.mjs";

const [inputFile] = process.argv.slice(2);
if (!inputFile) throw new Error("Uso: node tools/import-enem/verify-images.mjs <arquivo.json>");

const result = loadJson(inputFile).map((question, index) => ({
  index,
  title: question.title,
  referencedImages: [...(question.assets || []), ...(question.alternatives || []).flatMap((item) => item.assets || [])]
    .map((asset) => asset.url || asset.storagePath)
    .filter(Boolean),
}));

console.log(JSON.stringify(result, null, 2));
