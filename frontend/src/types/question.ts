import type { PageMetadata } from "@/types/api";

export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD";
export type MasteryStatus = "NEW" | "LEARNING" | "REVIEW" | "MASTERED";
export type ReviewPriority = "HIGH" | "MEDIUM" | "LOW";
export type QuestionImportStatus =
  | "DRAFT"
  | "NEEDS_REVIEW"
  | "VALIDATED"
  | "PUBLISHED"
  | "INVALID";
export type QuestionAssetType =
  | "IMAGE"
  | "GRAPH"
  | "TABLE"
  | "MAP"
  | "CHART"
  | "DIAGRAM"
  | "FORMULA"
  | "OTHER";

export interface QuestionAsset {
  id: number;
  questionId: number | null;
  alternativeId: number | null;
  type: QuestionAssetType;
  url: string | null;
  storagePath: string | null;
  originalFileName: string | null;
  sourcePage: number | null;
  cropX: number | null;
  cropY: number | null;
  cropWidth: number | null;
  cropHeight: number | null;
  altText: string | null;
  caption: string | null;
  checksum: string | null;
}

export interface Alternative {
  id: number;
  letter: string;
  text: string;
  html: string | null;
  assets: QuestionAsset[];
}

export interface Question {
  id: number;
  title: string;
  statement: string;
  statementHtml: string | null;
  imageUrl: string | null;
  subject: string;
  topic: string;
  subtopic: string | null;
  difficulty: DifficultyLevel;
  year: number;
  exam: string;
  competency: string | null;
  ability: string | null;
  source: string;
  sourceUrl: string;
  sourceExam: string;
  sourceYear: number;
  sourceQuestionNumber: number | null;
  sourceBookColor: string | null;
  sourceDay: number | null;
  sourcePage: number | null;
  officialSourceUrl: string | null;
  officialPdfUrl: string | null;
  officialAnswerKeyUrl: string | null;
  officialPage: number | null;
  validatedAgainstOfficialSource: boolean;
  validatedAt: string | null;
  externalProvider: string | null;
  externalProviderUrl: string | null;
  externalQuestionId: string | null;
  externalLicense: string | null;
  statementHash: string;
  importStatus: QuestionImportStatus;
  explanation: string | null;
  correctAlternative: string | null;
  favorite: boolean;
  answered: boolean | null;
  answeredCorrectly: boolean | null;
  assets: QuestionAsset[];
  alternatives: Alternative[];
  createdAt: string;
  updatedAt: string;
}

export interface ReviewQuestionSummary {
  id: number;
  title: string;
  source: string;
  sourceYear: number | null;
  sourceQuestionNumber: number | null;
  sourceBookColor: string | null;
  sourceDay: number | null;
  importStatus: QuestionImportStatus;
  validatedAgainstOfficialSource: boolean;
  externalProvider: string | null;
  importBatchId: number | null;
  subject: string;
  difficulty: DifficultyLevel;
  createdAt: string;
  importedAt: string | null;
  alternativesCount: number;
  assetsCount: number;
}

export interface ReviewQuestionDetail extends Omit<Question, "favorite" | "answered" | "answeredCorrectly"> {
  importedAt: string | null;
  importBatchId: number | null;
  alternativesCount: number;
  assetsCount: number;
}

export interface ReviewQuestionFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "ASC" | "DESC";
  status?: QuestionImportStatus | "";
  source?: string;
  year?: number | "";
  subject?: string;
}

export interface ReviewQuestionListResponse {
  items: ReviewQuestionSummary[];
  metadata: PageMetadata;
}

export interface ReviewQuestionStatusPayload {
  importStatus: Exclude<QuestionImportStatus, "PUBLISHED">;
}

export interface ReviewOfficialValidationPayload {
  officialSourceUrl?: string;
  officialPdfUrl?: string;
  officialAnswerKeyUrl?: string;
  officialPage?: number | null;
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
