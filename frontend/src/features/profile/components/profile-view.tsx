"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useProfile, useStatistics, useUpdateProfile } from "@/hooks/use-profile";
import { formatPercentage, formatSecondsToMinutes } from "@/lib/utils";
import type { UpdateProfilePayload } from "@/types/user";

export function ProfileView() {
  const { data: profile, isLoading: isProfileLoading, isError: isProfileError, refetch: refetchProfile } = useProfile();
  const {
    data: statistics,
    isLoading: isStatisticsLoading,
    isError: isStatisticsError,
    refetch: refetchStatistics,
  } = useStatistics();
  const { mutate, isPending } = useUpdateProfile();
  const { register, handleSubmit, reset } = useForm<UpdateProfilePayload>();

  useEffect(() => {
    if (profile) {
      reset({
        fullName: profile.fullName,
        bio: profile.bio ?? "",
        targetCourse: profile.targetCourse ?? "",
      });
    }
  }, [profile, reset]);

  if (isProfileLoading && isStatisticsLoading) {
    return <EmptyState title="Carregando perfil..." description="Buscando seus dados acadêmicos e preferências." />;
  }

  if (isProfileError || isStatisticsError) {
    return (
      <ErrorState
        title="Não foi possível carregar o perfil."
        description="Tente novamente para sincronizar seus dados e estatísticas."
        onRetry={() => {
          void refetchProfile();
          void refetchStatistics();
        }}
      />
    );
  }

  if (!profile) {
    return (
      <EmptyState
        title="Perfil indisponível."
        description="Sua sessão foi carregada, mas os dados do usuário ainda não voltaram da API."
      />
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Perfil"
        title="Dados pessoais, meta de aprovação e visão rápida do seu histórico."
        description="Atualize seu perfil, mantenha o foco do curso-alvo e acompanhe indicadores sem sair da área do aluno."
      />

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardHeader>
            <CardTitle>Atualizar perfil</CardTitle>
            <CardDescription>
              Informações conectadas à API `/users/me` e prontas para persistência real.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit((values) => mutate(values))}>
              <div className="space-y-2">
                <Label htmlFor="fullName">Nome completo</Label>
                <Input id="fullName" {...register("fullName")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="targetCourse">Curso alvo</Label>
                <Input id="targetCourse" {...register("targetCourse")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bio">Bio</Label>
                <Textarea id="bio" {...register("bio")} />
              </div>
              <Button type="submit" disabled={isPending}>
                {isPending ? "Salvando..." : "Salvar alterações"}
              </Button>
            </form>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Resumo acadêmico</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-[24px] bg-primary/8 p-5">
                <p className="text-sm text-muted-foreground">Precisão</p>
                <p className="mt-2 text-3xl font-bold">
                  {statistics ? formatPercentage(statistics.accuracyRate) : "--"}
                </p>
              </div>
              <div className="rounded-[24px] bg-secondary/8 p-5">
                <p className="text-sm text-muted-foreground">Tempo médio</p>
                <p className="mt-2 text-3xl font-bold">
                  {statistics
                    ? formatSecondsToMinutes(Math.round(statistics.averageTimeSpentSeconds))
                    : "--"}
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Conta</CardTitle>
              <CardDescription>
                Informações vindas do payload autenticado e do backend.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Email</span>
                <span>{profile.email}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Username</span>
                <span>{profile.username}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Status</span>
                <Badge variant={profile.active ? "success" : "outline"}>
                  {profile.active ? "Ativa" : "Inativa"}
                </Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Perfil</span>
                <span>{profile.roles.join(", ")}</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
