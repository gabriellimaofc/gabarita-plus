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
import { useLogin } from "@/hooks/use-auth";

const loginSchema = z.object({
  usernameOrEmail: z.string().min(3, "Informe email ou username."),
  password: z.string().min(8, "A senha precisa ter pelo menos 8 caracteres."),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginForm() {
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get("redirectTo");
  const login = useLogin(redirectTo);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: "",
      password: "",
    },
  });

  const errorMessage = login.error
    ? getErrorMessage(login.error, "NÃ£o foi possÃ­vel entrar agora.")
    : null;

  return (
    <AuthShell
      title="Entrar"
      description="Acesse seu painel, retome seus simulados e mantenha sua trilha de evoluÃ§Ã£o em dia."
      footer={
        <span>
          Ainda nÃ£o tem conta?{" "}
          <Link
            href={redirectTo ? `/cadastro?redirectTo=${encodeURIComponent(redirectTo)}` : "/cadastro"}
            className="font-semibold text-primary"
          >
            Criar cadastro
          </Link>
        </span>
      }
    >
      <form className="space-y-5" onSubmit={handleSubmit((values) => login.mutate(values))}>
        {errorMessage ? (
          <div
            role="alert"
            className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
          >
            {errorMessage}
          </div>
        ) : null}
        <div className="space-y-2">
          <Label htmlFor="usernameOrEmail">Email ou username</Label>
          <Input
            id="usernameOrEmail"
            placeholder="voce@email.com"
            {...register("usernameOrEmail")}
          />
          {errors.usernameOrEmail ? (
            <p className="text-sm text-rose-500">{errors.usernameOrEmail.message}</p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="password">Senha</Label>
          <Input id="password" type="password" {...register("password")} />
          {errors.password ? (
            <p className="text-sm text-rose-500">{errors.password.message}</p>
          ) : null}
        </div>
        <Button type="submit" size="lg" className="w-full" disabled={login.isPending}>
          {login.isPending ? "Entrando..." : "Entrar na plataforma"}
        </Button>
      </form>
    </AuthShell>
  );
}
