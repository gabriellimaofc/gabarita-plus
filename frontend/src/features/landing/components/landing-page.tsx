import Link from "next/link";
import {
  ArrowRight,
  BrainCircuit,
  ChartColumn,
  CircleCheckBig,
  NotebookPen,
  Rocket,
  ShieldCheck,
  Target,
} from "lucide-react";

import { Brand } from "@/components/common/brand";
import { ThemeToggle } from "@/components/common/theme-toggle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

const benefits = [
  {
    title: "Banco de questões inteligente",
    description: "Filtros avançados, feedback imediato e foco no que mais cai.",
    icon: BrainCircuit,
  },
  {
    title: "Dashboard com performance real",
    description: "Acompanhe precisão, ritmo, evolução semanal e gargalos.",
    icon: ChartColumn,
  },
  {
    title: "Caderno de erros acionável",
    description: "Transforme falhas em revisões organizadas e consistentes.",
    icon: NotebookPen,
  },
];

const proofPoints = [
  "Rotina mobile first pensada para estudo rápido",
  "Dark mode preparado para longas sessões",
  "Arquitetura pronta para integração com JWT e API REST",
  "Experiência SaaS premium com foco em velocidade",
];

export function LandingPage() {
  return (
    <div className="relative overflow-hidden">
      <header className="container-app py-5">
        <div className="glass-panel flex items-center justify-between rounded-[28px] px-4 py-3 sm:px-6">
          <Brand />
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Link href="/login">
              <Button variant="outline">Entrar</Button>
            </Link>
            <Link href="/cadastro">
              <Button>Criar conta</Button>
            </Link>
          </div>
        </div>
      </header>

      <main className="container-app pb-20 pt-8 sm:pt-12">
        <section className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div className="space-y-8">
            <Badge variant="secondary" className="px-4 py-1.5">
              Preparação inteligente para ENEM
            </Badge>
            <div className="space-y-5">
              <h1 className="max-w-3xl text-4xl font-black tracking-tight text-balance sm:text-5xl lg:text-6xl">
                Seu estudo deixa de ser aleatório e passa a evoluir com clareza.
              </h1>
              <p className="max-w-2xl text-base leading-8 text-muted-foreground sm:text-lg">
                O Gabarita+ organiza questões, simulados, estatísticas e revisão
                estratégica em uma plataforma feita para quem quer consistência,
                velocidade e leitura real de desempenho.
              </p>
            </div>
            <div className="flex flex-col gap-3 sm:flex-row">
              <Link href="/cadastro">
                <Button size="lg" className="w-full sm:w-auto">
                  Começar agora
                  <ArrowRight className="size-4" />
                </Button>
              </Link>
              <Link href="/dashboard">
                <Button size="lg" variant="outline" className="w-full sm:w-auto">
                  Ver experiência
                </Button>
              </Link>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {proofPoints.map((item) => (
                <div key={item} className="flex items-start gap-3 rounded-2xl border border-border/70 bg-background/60 p-4">
                  <CircleCheckBig className="mt-0.5 size-4 text-primary" />
                  <p className="text-sm text-muted-foreground">{item}</p>
                </div>
              ))}
            </div>
          </div>

          <Card className="overflow-hidden border-none bg-gradient-to-br from-slate-950 via-blue-950 to-violet-900 text-white shadow-[0_30px_120px_-35px_rgba(52,92,255,0.65)]">
            <CardContent className="p-0">
              <div className="grid gap-5 p-6 sm:p-8">
                <div className="rounded-[28px] bg-white/10 p-5 backdrop-blur">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-white/70">Precisão acumulada</p>
                      <p className="mt-2 text-4xl font-black">78,4%</p>
                    </div>
                    <div className="rounded-2xl bg-emerald-400/20 px-3 py-2 text-sm font-semibold text-emerald-200">
                      +12% este mês
                    </div>
                  </div>
                </div>
                <div className="grid gap-5 sm:grid-cols-2">
                  <div className="rounded-[24px] bg-white/10 p-5">
                    <Target className="size-5 text-blue-200" />
                    <p className="mt-4 text-sm text-white/70">Questões respondidas</p>
                    <p className="mt-1 text-3xl font-bold">1.248</p>
                  </div>
                  <div className="rounded-[24px] bg-white/10 p-5">
                    <Rocket className="size-5 text-violet-200" />
                    <p className="mt-4 text-sm text-white/70">Simulados concluídos</p>
                    <p className="mt-1 text-3xl font-bold">18</p>
                  </div>
                </div>
                <div className="rounded-[28px] bg-white/10 p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-white/70">Revisão estratégica</p>
                      <p className="mt-2 text-2xl font-bold">Caderno de erros priorizado</p>
                    </div>
                    <ShieldCheck className="size-7 text-emerald-200" />
                  </div>
                  <div className="mt-5 space-y-3">
                    <div className="h-3 rounded-full bg-white/10">
                      <div className="h-3 w-[72%] rounded-full bg-gradient-to-r from-blue-400 to-violet-400" />
                    </div>
                    <p className="text-sm text-white/70">
                      Revisões ativas para tópicos com maior taxa de erro e maior
                      incidência no exame.
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        <section className="mt-20 grid gap-6 lg:grid-cols-3">
          {benefits.map((benefit) => {
            const Icon = benefit.icon;
            return (
              <Card key={benefit.title} className="glass-panel">
                <CardContent className="space-y-4 p-6">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/15 to-secondary/15 text-primary">
                    <Icon className="size-5" />
                  </div>
                  <h2 className="text-xl font-semibold">{benefit.title}</h2>
                  <p className="text-sm leading-7 text-muted-foreground">
                    {benefit.description}
                  </p>
                </CardContent>
              </Card>
            );
          })}
        </section>

        <section className="mt-20 rounded-[36px] bg-gradient-to-r from-primary to-secondary px-6 py-10 text-white shadow-[0_24px_80px_-40px_rgba(79,70,229,0.65)] sm:px-10">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-sm font-semibold uppercase tracking-[0.28em] text-white/70">
                Experiência premium
              </p>
              <h2 className="mt-3 text-3xl font-black text-balance sm:text-4xl">
                Uma base pronta para crescer com produto real, API real e uso real.
              </h2>
            </div>
            <Link href="/cadastro">
              <Button
                size="lg"
                variant="outline"
                className="border-white/20 bg-white/10 text-white hover:bg-white/15"
              >
                Criar minha conta
              </Button>
            </Link>
          </div>
        </section>
      </main>

      <footer className="container-app pb-10">
        <div className="flex flex-col gap-4 rounded-[28px] border border-border/70 bg-background/70 px-6 py-5 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 Gabarita+. Plataforma inteligente de estudos para ENEM.</p>
          <p>Frontend em Next.js, pronto para integração com Spring Boot.</p>
        </div>
      </footer>
    </div>
  );
}
