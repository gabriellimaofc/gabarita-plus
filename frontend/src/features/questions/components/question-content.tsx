"use client";

import { ExternalLink, X } from "lucide-react";
import { useState } from "react";

import { cn } from "@/lib/utils";
import type { Alternative, QuestionAsset } from "@/types/question";

const ALLOWED_HTML_TAGS = new Set([
  "a",
  "blockquote",
  "br",
  "caption",
  "code",
  "div",
  "em",
  "figcaption",
  "figure",
  "h1",
  "h2",
  "h3",
  "h4",
  "hr",
  "i",
  "img",
  "li",
  "ol",
  "p",
  "span",
  "strong",
  "sub",
  "sup",
  "table",
  "tbody",
  "td",
  "th",
  "thead",
  "tr",
  "u",
  "ul",
]);

const BLOCKED_IMAGE_PATTERN = /broken-image\.svg|imagem\s+quebrada/i;

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function normalizeImportedText(value: string) {
  return value
    .replace(/\r\n/g, "\n")
    .replace(/\\\[/g, "[")
    .replace(/\\\]/g, "]")
    .replace(/\[\s*\.{3}\s*\]/g, "[...]")
    .replace(/\[…\]/g, "[...]")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/\s+(Disponivel em:|Disponível em:|Fonte:)/gi, "\n\n$1")
    .replace(/\s+(Acesso em:)/gi, "\n$1")
    .trim();
}

function isSafeUrl(value: string) {
  try {
    const parsed = new URL(value, "https://gabaritaplus.local");
    return ["http:", "https:", "mailto:"].includes(parsed.protocol);
  } catch {
    return false;
  }
}

function sanitizeUrl(value: string) {
  const trimmed = value.trim();
  return isSafeUrl(trimmed) ? escapeHtml(trimmed) : "#";
}

function renderInlineMarkdown(value: string) {
  let html = escapeHtml(value);

  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_, alt: string, url: string) => {
    if (BLOCKED_IMAGE_PATTERN.test(url)) {
      return '<span class="question-image-placeholder">Imagem indisponivel aguardando revisao oficial.</span>';
    }

    return `<img src="${sanitizeUrl(url)}" alt="${escapeHtml(alt)}" loading="lazy" />`;
  });

  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label: string, url: string) => {
    return `<a href="${sanitizeUrl(url)}" target="_blank" rel="noreferrer">${escapeHtml(label)}</a>`;
  });

  html = html
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/__([^_]+)__/g, "<strong>$1</strong>")
    .replace(/(^|[^*])\*([^*\n]+)\*/g, "$1<em>$2</em>")
    .replace(/(^|[^_])_([^_\n]+)_/g, "$1<em>$2</em>");

  return html;
}

function paragraphClass(value: string, index: number, total: number) {
  if (/^(Disponivel em:|Disponível em:|Fonte:|Refer[eê]ncia:|Acesso em:)/i.test(value)) {
    return ' class="question-reference"';
  }

  if (
    index === total - 1 &&
    /^(A partir|Com base|De acordo|Nesse contexto|Nessas condi[cç][oõ]es|Assinale|Qual|O que|Infere-se|Conclui-se)/i.test(value)
  ) {
    return ' class="question-command"';
  }

  return "";
}

function markdownToSafeHtml(markdown: string) {
  const normalized = normalizeImportedText(markdown);
  const blocks = normalized.split(/\n{2,}/).map((block) => block.trim()).filter(Boolean);

  return blocks
    .map((block, index) => {
      const lines = block.split("\n").map((line) => line.trim()).filter(Boolean);
      const firstLine = lines[0] ?? "";

      if (/^#{1,4}\s+/.test(firstLine)) {
        const level = Math.min(firstLine.match(/^#+/)?.[0].length ?? 2, 4);
        return `<h${level}>${renderInlineMarkdown(firstLine.replace(/^#{1,4}\s+/, ""))}</h${level}>`;
      }

      if (lines.every((line) => /^[-*]\s+/.test(line))) {
        return `<ul>${lines.map((line) => `<li>${renderInlineMarkdown(line.replace(/^[-*]\s+/, ""))}</li>`).join("")}</ul>`;
      }

      if (lines.every((line) => /^\d+[.)]\s+/.test(line))) {
        return `<ol>${lines.map((line) => `<li>${renderInlineMarkdown(line.replace(/^\d+[.)]\s+/, ""))}</li>`).join("")}</ol>`;
      }

      if (lines.every((line) => /^>\s?/.test(line))) {
        return `<blockquote>${lines.map((line) => renderInlineMarkdown(line.replace(/^>\s?/, ""))).join("<br />")}</blockquote>`;
      }

      const content = lines.map(renderInlineMarkdown).join("<br />");
      return `<p${paragraphClass(block, index, blocks.length)}>${content}</p>`;
    })
    .join("");
}

function sanitizeHtml(html: string) {
  return normalizeImportedText(html)
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
    .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, "")
    .replace(/<!--[\s\S]*?-->/g, "")
    .replace(/<\/?([a-zA-Z0-9-]+)([^>]*)>/g, (match, rawTag: string, rawAttrs: string) => {
      const tag = rawTag.toLowerCase();
      const isClosing = match.startsWith("</");

      if (!ALLOWED_HTML_TAGS.has(tag)) {
        return "";
      }

      if (isClosing) {
        return `</${tag}>`;
      }

      const attrs: string[] = [];
      const attrPattern = /([a-zA-Z:-]+)\s*=\s*("([^"]*)"|'([^']*)'|([^\s"'=<>`]+))/g;
      let attrMatch: RegExpExecArray | null;

      while ((attrMatch = attrPattern.exec(rawAttrs)) !== null) {
        const name = attrMatch[1].toLowerCase();
        const value = attrMatch[3] ?? attrMatch[4] ?? attrMatch[5] ?? "";

        if (name.startsWith("on")) {
          continue;
        }

        if (tag === "a" && name === "href") {
          attrs.push(`href="${sanitizeUrl(value)}" target="_blank" rel="noreferrer"`);
          continue;
        }

        if (tag === "img" && name === "src") {
          if (BLOCKED_IMAGE_PATTERN.test(value)) {
            return '<span class="question-image-placeholder">Imagem indisponivel aguardando revisao oficial.</span>';
          }
          attrs.push(`src="${sanitizeUrl(value)}" loading="lazy"`);
          continue;
        }

        if (tag === "img" && name === "alt") {
          attrs.push(`alt="${escapeHtml(value)}"`);
          continue;
        }
      }

      return `<${tag}${attrs.length ? ` ${attrs.join(" ")}` : ""}>`;
    });
}

function renderQuestionMarkup(html?: string | null, fallbackText?: string | null) {
  const htmlCandidate = html?.trim();
  const fallbackCandidate = fallbackText?.trim();

  if (htmlCandidate && /<\/?[a-z][\s\S]*>/i.test(htmlCandidate)) {
    return sanitizeHtml(htmlCandidate);
  }

  if (htmlCandidate) {
    return markdownToSafeHtml(htmlCandidate);
  }

  return fallbackCandidate ? markdownToSafeHtml(fallbackCandidate) : "";
}

export function QuestionRichText({
  html,
  fallbackText,
  className,
}: {
  html?: string | null;
  fallbackText?: string | null;
  className?: string;
}) {
  const renderedHtml = renderQuestionMarkup(html, fallbackText);

  if (renderedHtml) {
    return (
      <div
        className={cn("question-html text-[0.98rem] leading-8 text-foreground", className)}
        dangerouslySetInnerHTML={{ __html: renderedHtml }}
      />
    );
  }

  return null;
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
        <QuestionRichText html={statementHtml} fallbackText={statement} />
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
      <QuestionRichText html={alternative.html} fallbackText={alternative.text} className="text-sm leading-7 text-muted-foreground" />
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
