"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider as NextThemeProvider } from "next-themes";
import { Toaster } from "sonner";

import { queryClient } from "@/lib/query-client";
import { SessionProvider } from "@/providers/session-provider";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <NextThemeProvider attribute="class" defaultTheme="system" enableSystem>
      <QueryClientProvider client={queryClient}>
        <SessionProvider>{children}</SessionProvider>
        <Toaster richColors closeButton position="top-right" />
      </QueryClientProvider>
    </NextThemeProvider>
  );
}
