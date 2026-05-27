"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

import { AuthShell } from "@/features/auth/components/auth-shell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getErrorMessage } from "@/lib/api-error";
import { useRegister } from "@/hooks/use-auth";

const registerSchema = z.object({
  fullName: z.string().min(3, "Informe seu nome completo."),
  email: z.string().email("Digite um email vÃ¡lido."),
  username: z.string().min(3, "Use pelo menos 3 caracteres."),
  password: z.string().min(8, "A senha precisa ter no mÃ­nimo 8 caracteres."),
  targetCourse: z.string().optional(),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get("redirectTo");
  const registerUser = useRegister(redirectTo);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  const errorMessage = registerUser.error
    ? getErrorMessage(registerUser.error, "NÃ£o foi possÃ­vel criar sua conta.")
    : null;

  return (
    <AuthShell
      title="Criar conta"
      description="Monte sua base de estudos com uma experiÃªncia pronta para crescer com vocÃª."
      footer={
        <span>
          JÃ¡ tem conta?{" "}
          <Link
            href={redirectTo ? `/login?redirectTo=${encodeURIComponent(redirectTo)}` : "/login"}
            className="font-semibold text-primary"
          >
            Fazer login
          </Link>
        </span>
      }
    >
      <form
        className="grid gap-5 sm:grid-cols-2"
        onSubmit={handleSubmit((values) => registerUser.mutate(values))}
      >
        {errorMessage ? (
          <div
            role="alert"
            className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100 sm:col-span-2"
          >
            {errorMessage}
          </div>
        ) : null}
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="fullName">Nome completo</Label>
          <Input id="fullName" {...register("fullName")} />
          {errors.fullName ? (
            <p className="text-sm text-rose-500">{errors.fullName.message}</p>
          ) : null}
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" {...register("email")} />
          {errors.email ? (
            <p className="text-sm text-rose-500">{errors.email.message}</p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="username">Username</Label>
          <Input id="username" {...register("username")} />
          {errors.username ? (
            <p className="text-sm text-rose-500">{errors.username.message}</p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="targetCourse">Curso alvo</Label>
          <Input
            id="targetCourse"
            placeholder="Medicina, Direito..."
            {...register("targetCourse")}
          />
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="password">Senha</Label>
          <Input id="password" type="password" {...register("password")} />
          {errors.password ? (
            <p className="text-sm text-rose-500">{errors.password.message}</p>
          ) : null}
        </div>
        <div className="sm:col-span-2">
          <Button
            type="submit"
            size="lg"
            className="w-full"
            disabled={registerUser.isPending}
          >
            {registerUser.isPending ? "Criando conta..." : "Criar conta e entrar"}
          </Button>
        </div>
      </form>
    </AuthShell>
  );
}
