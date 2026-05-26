"use client";

import { useSessionBootstrap } from "@/hooks/use-session-bootstrap";

export function SessionProvider({ children }: { children: React.ReactNode }) {
  useSessionBootstrap();

  return children;
}
