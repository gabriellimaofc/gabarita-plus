"use client";

import { ExternalLink, X } from "lucide-react";
import { useState } from "react";

import { cn } from "@/lib/utils";
import type { Alternative, QuestionAsset } from "@/types/question";

function HtmlBlock({
  html,
  fallbackText,
  className,
}: {
  html?: string | null;
  fallbackText?: string | null;
  className?: string;
}) {
  if (html) {
    return (
      <div
        className={cn("question-html space-y-4 text-sm leading-8 text-foreground", className)}
        dangerouslySetInnerHTML={{ __html: html }}
      />
    );
  }

  if (!fallbackText) {
    return null;
  }

  return <p className={cn("text-sm leading-8 text-muted-foreground", className)}>{fallbackText}</p>;
}

function AssetImage({
  asset,
  onOpen,
}: {
  asset: QuestionAsset;
  onOpen: (asset: QuestionAsset) => void;
}) {
  const [broken, setBroken] = useState(false);

  if (!asset.url || broken) {
    return (
      <div className="rounded-[22px] border border-dashed border-border bg-muted/30 p-4 text-sm text-muted-foreground">
        Nao foi possivel carregar este recurso visual.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-[24px] border border-border/70 bg-background/80">
      <button
        type="button"
        className="block w-full"
        onClick={() => onOpen(asset)}
        aria-label={asset.altText ?? "Ampliar imagem da questao"}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={asset.url}
          alt={asset.altText ?? "Recurso visual da questao"}
          loading="lazy"
          className="max-h-[420px] w-full object-contain bg-muted/20"
          onError={() => setBroken(true)}
        />
      </button>
      <div className="flex items-start justify-between gap-3 p-4 text-sm text-muted-foreground">
        <div>
          <p className="font-medium text-foreground">{asset.type}</p>
          {asset.caption ? <p className="mt-1">{asset.caption}</p> : null}
        </div>
        {asset.url ? (
          <a href={asset.url} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-primary">
            Abrir
            <ExternalLink className="size-3.5" />
          </a>
        ) : null}
      </div>
    </div>
  );
}

function AssetGallery({
  assets,
  title,
  onOpen,
}: {
  assets: QuestionAsset[];
  title?: string;
  onOpen: (asset: QuestionAsset) => void;
}) {
  if (!assets.length) {
    return null;
  }

  return (
    <div className="space-y-3">
      {title ? <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">{title}</p> : null}
      <div className="grid gap-4">
        {assets.map((asset) => (
          <AssetImage key={asset.id} asset={asset} onOpen={onOpen} />
        ))}
      </div>
    </div>
  );
}

export function QuestionContent({
  statement,
  statementHtml,
  assets,
  sourceLabel,
}: {
  statement: string;
  statementHtml?: string | null;
  assets: QuestionAsset[];
  sourceLabel?: string;
}) {
  const [activeAsset, setActiveAsset] = useState<QuestionAsset | null>(null);

  return (
    <>
      <div className="space-y-5">
        <HtmlBlock html={statementHtml} fallbackText={statement} />
        <AssetGallery assets={assets} title={sourceLabel} onOpen={setActiveAsset} />
      </div>

      {activeAsset?.url ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4">
          <div className="relative w-full max-w-5xl rounded-[28px] bg-background p-4 shadow-2xl">
            <button
              type="button"
              onClick={() => setActiveAsset(null)}
              className="absolute right-3 top-3 rounded-full border border-border bg-background p-2"
              aria-label="Fechar imagem ampliada"
            >
              <X className="size-4" />
            </button>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={activeAsset.url}
              alt={activeAsset.altText ?? "Imagem ampliada da questao"}
              className="max-h-[82vh] w-full object-contain"
            />
            {activeAsset.caption ? (
              <p className="mt-3 text-sm text-muted-foreground">{activeAsset.caption}</p>
            ) : null}
          </div>
        </div>
      ) : null}
    </>
  );
}

export function AlternativeContent({ alternative }: { alternative: Alternative }) {
  return (
    <div className="space-y-3">
      <HtmlBlock html={alternative.html} fallbackText={alternative.text} className="text-sm leading-7 text-muted-foreground" />
      {alternative.assets.length ? (
        <div className="grid gap-3">
          {alternative.assets.map((asset) => (
            <div key={asset.id} className="overflow-hidden rounded-[20px] border border-border/70 bg-background/70">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={asset.url ?? ""}
                alt={asset.altText ?? `Recurso visual da alternativa ${alternative.letter}`}
                className="max-h-64 w-full object-contain bg-muted/20"
              />
              {asset.caption ? <p className="p-3 text-sm text-muted-foreground">{asset.caption}</p> : null}
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
