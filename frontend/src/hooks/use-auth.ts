"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { getErrorMessage, toAppError } from "@/lib/api-error";
import { authService } from "@/services/auth.service";
import { useAuthStore } from "@/store/auth-store";
import type { LoginPayload, RegisterPayload } from "@/types/auth";

export function useLogin() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);

  return useMutation({
    mutationFn: (payload: LoginPayload) => authService.login(payload),
    onSuccess: (data) => {
      setSession(data);
      toast.success("Acesso liberado. Bem-vindo ao Gabarita+.");
      router.push("/dashboard");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível entrar agora.")),
  });
}

export function useRegister() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);

  return useMutation({
    mutationFn: (payload: RegisterPayload) => authService.register(payload),
    onSuccess: (data) => {
      setSession(data);
      toast.success("Conta criada com sucesso.");
      router.push("/dashboard");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível criar sua conta.")),
  });
}

export function useLogout() {
  const clearSession = useAuthStore((state) => state.clearSession);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const router = useRouter();

  return useMutation({
    mutationFn: async () => authService.logout(refreshToken ?? ""),
    onSettled: () => {
      clearSession();
      toast.success("Sessão encerrada.");
      router.push("/login");
    },
    onError: (error) => {
      const appError = toAppError(error);
      if (appError.status && appError.status >= 500) {
        toast.error(appError.message);
      }
    },
  });
}
