"use client";

import { Clock3 } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { cn } from "@/lib/utils";

function formatTime(totalSeconds: number) {
  const safeSeconds = Math.max(totalSeconds, 0);
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const seconds = safeSeconds % 60;

  if (hours > 0) {
    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function ExamTimer({
  examId,
  durationMinutes,
  finished,
  onElapsedChange,
  onExpire,
}: {
  examId: number;
  durationMinutes: number;
  finished: boolean;
  onElapsedChange?: (elapsedSeconds: number) => void;
  onExpire?: () => void;
}) {
  const totalSeconds = durationMinutes * 60;
  const [remainingSeconds, setRemainingSeconds] = useState(totalSeconds);
  const expiredRef = useRef(false);
  const onElapsedChangeRef = useRef(onElapsedChange);
  const onExpireRef = useRef(onExpire);

  useEffect(() => {
    onElapsedChangeRef.current = onElapsedChange;
  }, [onElapsedChange]);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    const storageKey = `gp_mock_exam_started_at_${examId}`;

    if (finished) {
      window.localStorage.removeItem(storageKey);
      setRemainingSeconds(totalSeconds);
      expiredRef.current = false;
      return;
    }

    const storedStartedAt = window.localStorage.getItem(storageKey);
    const startedAt = storedStartedAt ? Number(storedStartedAt) : Date.now();
    expiredRef.current = false;
    if (!storedStartedAt) {
      window.localStorage.setItem(storageKey, String(startedAt));
    }

    const updateTimer = () => {
      const elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000);
      const nextRemainingSeconds = Math.max(totalSeconds - elapsedSeconds, 0);
      setRemainingSeconds(nextRemainingSeconds);
      onElapsedChangeRef.current?.(Math.max(totalSeconds - nextRemainingSeconds, 0));

      if (nextRemainingSeconds <= 0 && !expiredRef.current) {
        expiredRef.current = true;
        onExpireRef.current?.();
      }
    };

    updateTimer();
    const intervalId = window.setInterval(updateTimer, 1000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [examId, finished, totalSeconds]);

  const isLowTime = remainingSeconds <= 5 * 60;

  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-[24px] border px-4 py-3",
        isLowTime ? "border-amber-500/40 bg-amber-500/10" : "border-border/70 bg-background/80",
      )}
    >
      <Clock3 className={cn("size-5", isLowTime ? "text-amber-500" : "text-primary")} />
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          Tempo restante
        </p>
        <p className="text-lg font-semibold">{formatTime(remainingSeconds)}</p>
      </div>
    </div>
  );
}
