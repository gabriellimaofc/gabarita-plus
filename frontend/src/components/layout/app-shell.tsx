"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, LogOut, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";

import { Brand } from "@/components/common/brand";
import { ThemeToggle } from "@/components/common/theme-toggle";
import { Button } from "@/components/ui/button";
import { cn, initialsFromName } from "@/lib/utils";
import { navItems } from "@/lib/constants";
import { useLogout } from "@/hooks/use-auth";
import { useAuthStore } from "@/store/auth-store";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { mutate: logout, isPending } = useLogout();
  const user = useAuthStore((state) => state.user);

  const initials = useMemo(
    () => initialsFromName(user?.fullName ?? "Gabarita Plus"),
    [user?.fullName],
  );

  return (
    <div className="min-h-screen">
      <div className="container-app flex min-h-screen gap-6 py-4 lg:py-6">
        <aside
          className={cn(
            "glass-panel fixed inset-y-4 left-4 z-50 flex w-[280px] flex-col rounded-[32px] p-4 transition-transform duration-300 lg:static lg:translate-x-0",
            sidebarOpen ? "translate-x-0" : "-translate-x-[120%]",
          )}
        >
          <div className="flex items-center justify-between">
            <Brand />
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden"
              onClick={() => setSidebarOpen(false)}
            >
              <span className="text-lg">×</span>
            </Button>
          </div>

          <div className="mt-6 rounded-[28px] bg-gradient-to-br from-primary/12 via-primary/8 to-secondary/12 p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
              Sua jornada
            </p>
            <p className="mt-2 text-lg font-semibold">
              Rotina guiada por dados, com foco total no ENEM.
            </p>
            <div className="mt-4 inline-flex items-center gap-2 rounded-full bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground">
              <Sparkles className="size-3.5 text-secondary" />
              Sprint premium de aprendizado
            </div>
          </div>

          <nav className="mt-6 space-y-2">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center rounded-2xl px-4 py-3 text-sm font-medium transition",
                  pathname === item.href || pathname.startsWith(`${item.href}/`)
                    ? "bg-primary text-primary-foreground shadow-lg shadow-blue-500/20"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                )}
                onClick={() => setSidebarOpen(false)}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className="mt-auto space-y-4 rounded-[28px] border border-border/70 bg-background/70 p-4">
            <div className="flex items-center gap-3">
              <div className="flex size-11 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-secondary text-sm font-bold text-white">
                {initials}
              </div>
              <div>
                <p className="font-semibold">{user?.fullName ?? "Aluno ENEM"}</p>
                <p className="text-xs text-muted-foreground">
                  {user?.targetCourse ?? "Plano focado em aprovação"}
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="outline"
              className="w-full justify-start"
              onClick={() => logout()}
              disabled={isPending}
            >
              <LogOut className="size-4" />
              Sair
            </Button>
          </div>
        </aside>

        {sidebarOpen ? (
          <button
            type="button"
            className="fixed inset-0 z-40 bg-slate-950/40 lg:hidden"
            onClick={() => setSidebarOpen(false)}
            aria-label="Fechar menu"
          />
        ) : null}

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="glass-panel sticky top-4 z-30 mb-6 flex items-center justify-between rounded-[28px] px-4 py-3 sm:px-5">
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="icon"
                className="lg:hidden"
                onClick={() => setSidebarOpen(true)}
              >
                <Menu className="size-4" />
              </Button>
              <div>
                <p className="text-sm text-muted-foreground">Plataforma inteligente</p>
                <p className="font-semibold">Foco, consistência e desempenho</p>
              </div>
            </div>
            <ThemeToggle />
          </header>

          <main className="pb-8">{children}</main>
        </div>
      </div>
    </div>
  );
}
