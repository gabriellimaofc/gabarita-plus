"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getErrorMessage } from "@/lib/api-error";
import { userService } from "@/services/user.service";
import { useAuthStore } from "@/store/auth-store";
import type { UpdateProfilePayload } from "@/types/user";

export function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: () => userService.getProfile(),
  });
}

export function useStatistics() {
  return useQuery({
    queryKey: ["statistics"],
    queryFn: () => userService.getStatistics(),
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  const updateUser = useAuthStore((state) => state.updateUser);

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => userService.updateProfile(payload),
    onSuccess: (profile) => {
      queryClient.invalidateQueries({ queryKey: ["profile"] });
      updateUser(profile);
      toast.success("Perfil atualizado.");
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, "Não foi possível salvar o perfil.")),
  });
}
