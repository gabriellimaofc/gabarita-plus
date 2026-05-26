import { Skeleton } from "@/components/ui/skeleton";

export function PageShellSkeleton() {
  return (
    <div className="container-app space-y-8 py-10">
      <Skeleton className="h-16 w-full" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-48 lg:col-span-2" />
        <Skeleton className="h-48" />
      </div>
      <Skeleton className="h-80 w-full" />
    </div>
  );
}
