import { Brand } from "@/components/common/brand";
import { ThemeToggle } from "@/components/common/theme-toggle";
import { Card, CardContent } from "@/components/ui/card";

export function AuthShell({
  title,
  description,
  footer,
  children,
}: {
  title: string;
  description: string;
  footer: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="container-app flex min-h-screen items-center py-10">
      <div className="grid w-full gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
        <div className="hidden lg:block">
          <div className="space-y-6">
            <Brand />
            <div className="space-y-4">
              <h1 className="text-5xl font-black tracking-tight text-balance">
                Preparação inteligente, bonita e feita para durar.
              </h1>
              <p className="max-w-xl text-base leading-8 text-muted-foreground">
                Entre no Gabarita+ e acompanhe o que acelera sua aprovação com
                estatísticas, filtros, simulados e revisão estratégica.
              </p>
            </div>
            <div className="glass-panel rounded-[32px] p-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-[24px] bg-primary/8 p-5">
                  <p className="text-sm text-muted-foreground">Experiência conectada</p>
                  <p className="mt-2 text-3xl font-bold">JWT real</p>
                </div>
                <div className="rounded-[24px] bg-secondary/8 p-5">
                  <p className="text-sm text-muted-foreground">Backend</p>
                  <p className="mt-2 text-3xl font-bold">Spring Boot</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="w-full max-w-xl justify-self-center">
          <div className="mb-5 flex items-center justify-between lg:hidden">
            <Brand />
            <ThemeToggle />
          </div>
          <Card className="glass-panel border-white/10">
            <CardContent className="p-8 sm:p-10">
              <div className="mb-8 flex items-start justify-between gap-4">
                <div className="space-y-2">
                  <h2 className="text-3xl font-bold tracking-tight">{title}</h2>
                  <p className="text-sm leading-7 text-muted-foreground">{description}</p>
                </div>
                <div className="hidden lg:block">
                  <ThemeToggle />
                </div>
              </div>
              {children}
              <div className="mt-6 text-sm text-muted-foreground">{footer}</div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
