import { fetchYears } from "./lib/enem-dev-utils.mjs";
import { spawn } from "node:child_process";

const years = await fetchYears();
console.log(`Anos encontrados: ${years.length}`);

for (const yearEntry of years) {
  const year = yearEntry.year;
  await new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      ["tools/import-enem/enem-dev/fetch-year.mjs", String(year)],
      { stdio: "inherit" },
    );
    child.on("exit", (code) => (code === 0 ? resolve() : reject(new Error(`Falha ao baixar ${year}`))));
  });
}
