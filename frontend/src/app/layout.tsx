import type { Metadata } from "next";

import { AppProviders } from "@/providers/app-providers";
import { cn } from "@/lib/utils";

import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL("https://gabarita.plus"),
  title: {
    default: "Gabarita+ | Plataforma inteligente para ENEM",
    template: "%s | Gabarita+",
  },
  description:
    "Estude para o ENEM com questões, simulados, análise de desempenho e caderno de erros em uma experiência SaaS premium.",
  keywords: [
    "ENEM",
    "plataforma de estudos",
    "questões ENEM",
    "simulados",
    "caderno de erros",
  ],
  openGraph: {
    title: "Gabarita+",
    description:
      "Plataforma inteligente para evoluir sua preparação no ENEM com ritmo, clareza e performance.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={cn("min-h-screen bg-background font-sans text-foreground")}>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
