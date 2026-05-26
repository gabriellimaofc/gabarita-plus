"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";

import { toAppError } from "@/lib/api-error";
import { userService } from "@/services/user.service";
import { useAuthStore } from "@/store/auth-store";

export function useSessionBootstrap() {
  const hydrated = useAuthStore((state) => state.hydrated);
  const accessToken = useAuthStore((state) => state.accessToken);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const clearSession = useAuthStore((state) => state.clearSession);

  const query = useQuery({
    queryKey: ["session-bootstrap"],
    queryFn: () => userService.getProfile(),
    enabled: hydrated && Boolean(accessToken || refreshToken) && !user,
    staleTime: 5 * 60_000,
    retry: false,
  });

  useEffect(() => {
    if (query.data) {
      updateUser(query.data);
    }
  }, [query.data, updateUser]);

  useEffect(() => {
    if (query.isError) {
      const error = toAppError(query.error);
      if (error.status === 401 || error.status === 403) {
        clearSession();
      }
    }
  }, [clearSession, query.error, query.isError]);

  return query;
}
