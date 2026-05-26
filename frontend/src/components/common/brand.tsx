import Link from "next/link";
import { GraduationCap } from "lucide-react";

import { APP_NAME } from "@/lib/constants";

export function Brand() {
  return (
    <Link href="/" className="flex items-center gap-3">
      <div className="flex size-11 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-secondary text-white shadow-lg shadow-blue-500/20">
        <GraduationCap className="size-5" />
      </div>
      <div className="space-y-0.5">
        <p className="text-base font-bold tracking-tight">{APP_NAME}</p>
        <p className="text-xs text-muted-foreground">Estudo inteligente para ENEM</p>
      </div>
    </Link>
  );
}
