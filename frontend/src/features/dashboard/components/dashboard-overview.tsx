"use client";

import { BarChart3, Clock3, Crosshair, TrendingUp } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { DashboardSkeleton } from "@/components/common/dashboard-skeleton";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useDashboard } from "@/hooks/use-dashboard";
import { formatPercentage, formatSecondsToMinutes } from "@/lib/utils";

const statItems = [
  { key: "accuracyRate", label: "Precisão", icon: Crosshair },
  { key: "totalAnswered", label: "Respondidas", icon: BarChart3 },
  { key: "averageTimeSpentSeconds", label: "Tempo médio", icon: Clock3 },
  { key: "weeklyProgress", label: "Ritmo", icon: TrendingUp },
] as const;

export function DashboardOverview() {
  const { data, isLoading, isError, refetch } = useDashboard();

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (isError || !data) {
    return (
      <ErrorState
        title="Não foi possível carregar o dashboard."
        description="Verifique sua sessão e tente buscar os indicadores novamente."
        onRetry={() => void refetch()}
      />
    );
  }

  if (
    data.totalAnswered === 0 &&
    data.performanceBySubject.length === 0 &&
    data.weeklyProgress.length === 0
  ) {
    return (
      <EmptyState
        title="Seu dashboard ainda está vazio."
        description="Responda algumas questões para liberar gráficos, precisão e evolução semanal."
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Painel central"
        title="Seu estudo em uma visão clara e acionável."
        description="Acompanhe precisão, volume, ritmo e desempenho por matéria para decidir o próximo passo com confiança."
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {statItems.map((item) => {
          const Icon = item.icon;
          const value =
            item.key === "accuracyRate"
              ? formatPercentage(data.accuracyRate)
              : item.key === "averageTimeSpentSeconds"
                ? formatSecondsToMinutes(Math.round(data.averageTimeSpentSeconds))
                : item.key === "weeklyProgress"
                  ? `${data.weeklyProgress.at(-1)?.totalAnswers ?? 0} questões`
                  : new Intl.NumberFormat("pt-BR").format(data.totalAnswered);

          return (
            <Card key={item.key}>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="space-y-3">
                    <p className="text-sm text-muted-foreground">{item.label}</p>
                    <p className="text-3xl font-bold tracking-tight">{value}</p>
                  </div>
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <Icon className="size-5" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3">
          <CardHeader>
            <Badge variant="secondary" className="w-fit">
              Evolução semanal
            </Badge>
            <CardTitle>Ritmo de desempenho</CardTitle>
            <CardDescription>
              Compare volume de respostas e taxa de acerto por semana.
            </CardDescription>
          </CardHeader>
          <CardContent className="h-[320px] pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data.weeklyProgress}>
                <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.18} />
                <XAxis dataKey="week" strokeOpacity={0.5} />
                <YAxis strokeOpacity={0.5} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="totalAnswers"
                  stroke="hsl(var(--primary))"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                />
                <Line
                  type="monotone"
                  dataKey="correctAnswers"
                  stroke="hsl(var(--secondary))"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="xl:col-span-2">
          <CardHeader>
            <Badge className="w-fit">Assuntos</Badge>
            <CardTitle>Performance por matéria</CardTitle>
            <CardDescription>
              Encontre rapidamente os blocos que merecem mais energia.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            {data.performanceBySubject.map((subject) => (
              <div key={subject.name} className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="font-medium">{subject.name}</span>
                  <span className="text-muted-foreground">
                    {formatPercentage(subject.accuracy)}
                  </span>
                </div>
                <Progress value={subject.accuracy} />
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <Badge variant="outline" className="w-fit">
            Tópicos quentes
          </Badge>
          <CardTitle>Precisão por tópico</CardTitle>
          <CardDescription>
            Os tópicos mais sensíveis aparecem primeiro para guiar a revisão.
          </CardDescription>
        </CardHeader>
        <CardContent className="h-[320px] pt-2">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.performanceByTopic}>
              <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.18} />
              <XAxis dataKey="name" strokeOpacity={0.5} />
              <YAxis strokeOpacity={0.5} />
              <Tooltip />
              <Bar dataKey="accuracy" radius={[12, 12, 0, 0]}>
                {data.performanceByTopic.map((entry, index) => (
                  <Cell
                    key={entry.name}
                    fill={index % 2 === 0 ? "hsl(var(--primary))" : "hsl(var(--secondary))"}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
}
