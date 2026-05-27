import type {
  DifficultyLevel,
  ErrorNotebookFilters,
  MasteryStatus,
  ReviewPriority,
} from "@/types/question";

export function parseErrorNotebookFilters(searchParams: URLSearchParams): ErrorNotebookFilters {
  return {
    subject: searchParams.get("subject") ?? "",
    topic: searchParams.get("topic") ?? "",
    difficulty: (searchParams.get("difficulty") as DifficultyLevel | null) ?? "",
    masteryStatus:
      (searchParams.get("masteryStatus") as MasteryStatus | null) ?? "",
    priority: (searchParams.get("priority") as ReviewPriority | null) ?? "",
  };
}

export function buildErrorNotebookParams(filters: ErrorNotebookFilters) {
  const params = new URLSearchParams();

  if (filters.subject) {
    params.set("subject", filters.subject);
  }
  if (filters.topic) {
    params.set("topic", filters.topic);
  }
  if (filters.difficulty) {
    params.set("difficulty", filters.difficulty);
  }
  if (filters.masteryStatus) {
    params.set("masteryStatus", filters.masteryStatus);
  }
  if (filters.priority) {
    params.set("priority", filters.priority);
  }

  return params;
}
