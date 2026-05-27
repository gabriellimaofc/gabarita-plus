"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight, List } from "lucide-react";
import type { ReactNode } from "react";

import { Button } from "@/components/ui/button";

function NavigationAction({
  href,
  disabled,
  onClick,
  children,
}: {
  href?: string;
  disabled?: boolean;
  onClick?: () => void;
  children: ReactNode;
}) {
  if (href && !disabled) {
    return (
      <Link href={href}>
        <Button variant="outline">{children}</Button>
      </Link>
    );
  }

  return (
    <Button type="button" variant="outline" disabled={disabled} onClick={onClick}>
      {children}
    </Button>
  );
}

export function QuestionNavigator({
  currentIndex,
  totalCount,
  backHref,
  previousHref,
  nextHref,
  onPrevious,
  onNext,
  previousDisabled,
  nextDisabled,
  backLabel = "Voltar para lista",
  previousLabel = "Questao anterior",
  nextLabel = "Proxima questao",
}: {
  currentIndex: number;
  totalCount: number;
  backHref: string;
  previousHref?: string;
  nextHref?: string;
  onPrevious?: () => void;
  onNext?: () => void;
  previousDisabled?: boolean;
  nextDisabled?: boolean;
  backLabel?: string;
  previousLabel?: string;
  nextLabel?: string;
}) {
  return (
    <div className="flex flex-col gap-3 rounded-[28px] border border-border/70 bg-background/80 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-semibold">
          Questao {Math.max(currentIndex + 1, 1)} de {Math.max(totalCount, 1)}
        </p>
        <p className="text-sm text-muted-foreground">
          Continue no mesmo fluxo sem perder seus filtros.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <Link href={backHref}>
          <Button variant="outline">
            <List className="size-4" />
            {backLabel}
          </Button>
        </Link>

        <NavigationAction
          href={previousHref}
          disabled={previousDisabled ?? (!previousHref && !onPrevious)}
          onClick={onPrevious}
        >
          <ArrowLeft className="size-4" />
          {previousLabel}
        </NavigationAction>

        <NavigationAction
          href={nextHref}
          disabled={nextDisabled ?? (!nextHref && !onNext)}
          onClick={onNext}
        >
          {nextLabel}
          <ArrowRight className="size-4" />
        </NavigationAction>
      </div>
    </div>
  );
}
