import { QueryClient } from "@tanstack/react-query";

import { toAppError } from "@/lib/api-error";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: (failureCount, error) => {
        const appError = toAppError(error);

        if (!appError.status) {
          return failureCount < 2;
        }

        if ([400, 401, 403, 404, 409, 422].includes(appError.status)) {
          return false;
        }

        return failureCount < 2;
      },
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 5000),
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
