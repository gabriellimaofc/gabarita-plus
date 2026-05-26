"use client";

import { AppShell } from "@/components/layout/app-shell";
import { useProtectedRoute } from "@/hooks/use-protected-route";

export default function ProtectedLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  useProtectedRoute();

  return <AppShell>{children}</AppShell>;
}
