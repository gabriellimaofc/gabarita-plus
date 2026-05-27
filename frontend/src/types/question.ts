import type { PageMetadata } from "@/types/api";

export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD";
export type MasteryStatus = "NEW" | "LEARNING" | "REVIEW" | "MASTERED";
export type ReviewPriority = "HIGH" | "MEDIUM" | "LOW";

export interface Alternative {
  id: number;
  letter: string;
  text: string;
}

export interface Question {
  id: number;
  title: string;
  statement: string;
  imageUrl: string | null;
  subject: string;
  topic: string;
  subtopic: string | null;
  difficulty: DifficultyLevel;
  year: number;
  exam: string;
  competency: string | null;
  ability: string | null;
  explanation: string | null;
  correctAlternative: string | null;
  favorite: boolean;
  answered: boolean | null;
  answeredCorrectly: boolean | null;
  alternatives: Alternative[];
  createdAt: string;
  updatedAt: string;
}

export interface QuestionFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "ASC" | "DESC";
  search?: string;
  subject?: string;
  topic?: string;
  subtopic?: string;
  difficulty?: DifficultyLevel | "";
  year?: number | "";
  exam?: string;
  answered?: boolean | "";
  incorrectOnly?: boolean;
  favoritesOnly?: boolean;
}

export interface QuestionListResponse {
  items: Question[];
  metadata: PageMetadata;
}

export interface AnswerQuestionPayload {
  questionId: number;
  chosenAlternative: string;
  timeSpentSeconds: number;
}

export interface AnswerQuestionResult {
  id: number;
  questionId: number;
  chosenAlternative: string;
  correct: boolean;
  correctAlternative: string;
  explanation: string | null;
  attemptNumber: number;
  timeSpentSeconds: number;
  answeredAt: string;
}

export interface ErrorNotebookFilters {
  subject?: string;
  topic?: string;
  difficulty?: DifficultyLevel | "";
  masteryStatus?: MasteryStatus | "";
  priority?: ReviewPriority | "";
}

export interface ErrorNotebookEntry {
  id: number;
  questionId: number;
  questionTitle: string;
  subject: string;
  topic: string;
  difficulty: DifficultyLevel;
  errorCount: number;
  lastErrorAt: string | null;
  lastReviewedAt: string | null;
  nextReviewAt: string | null;
  masteryStatus: MasteryStatus;
  priority: ReviewPriority;
  updatedAt: string;
}

export interface UpdateErrorNotebookStatusPayload {
  masteryStatus: MasteryStatus;
}
