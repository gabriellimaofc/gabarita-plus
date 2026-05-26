import { Badge } from "@/components/ui/badge";

export function PageHeader({
  eyebrow,
  title,
  description,
}: {
  eyebrow?: string;
  title: string;
  description: string;
}) {
  return (
    <div className="space-y-3">
      {eyebrow ? <Badge variant="secondary">{eyebrow}</Badge> : null}
      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight text-balance sm:text-4xl">
          {title}
        </h1>
        <p className="max-w-3xl text-sm leading-6 text-muted-foreground sm:text-base">
          {description}
        </p>
      </div>
    </div>
  );
}
