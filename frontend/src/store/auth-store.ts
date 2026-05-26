"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

import {
  ACCESS_TOKEN_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
} from "@/lib/constants";
import { clearSessionCookies, setSessionCookies } from "@/lib/auth-session";
import type { AuthResponse } from "@/types/auth";
import type { UserProfile } from "@/types/user";

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  refreshToken: string | null;
  expiresAt: number | null;
  hydrated: boolean;
  setSession: (payload: AuthResponse) => void;
  updateUser: (user: UserProfile) => void;
  clearSession: () => void;
  markHydrated: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      expiresAt: null,
      hydrated: false,
      setSession: (payload) => {
        const expiresAt = Date.now() + payload.expiresIn * 1000;
        setSessionCookies(
          payload.accessToken,
          payload.refreshToken,
          payload.expiresIn,
        );

        set({
          user: payload.user,
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken,
          expiresAt,
        });
      },
      updateUser: (user) => {
        set({ user });
      },
      clearSession: () => {
        clearSessionCookies();
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          expiresAt: null,
        });
      },
      markHydrated: () => set({ hydrated: true }),
    }),
    {
      name: "gp-auth-store",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        expiresAt: state.expiresAt,
      }),
      onRehydrateStorage: () => (state) => {
        state?.markHydrated();
        if (state?.accessToken && state.refreshToken) {
          const remainingSeconds = state.expiresAt
            ? Math.max(Math.floor((state.expiresAt - Date.now()) / 1000), 60)
            : 3600;
          setSessionCookies(state.accessToken, state.refreshToken, remainingSeconds);
        }
      },
    },
  ),
);

export const authStorageKeys = {
  accessToken: ACCESS_TOKEN_STORAGE_KEY,
  refreshToken: REFRESH_TOKEN_STORAGE_KEY,
};
