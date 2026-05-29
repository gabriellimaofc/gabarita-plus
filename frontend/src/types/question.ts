import type { PageMetadata } from "@/types/api";

export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD";
export type MasteryStatus = "NEW" | "LEARNING" | "REVIEW" | "MASTERED";
export type ReviewPriority = "HIGH" | "MEDIUM" | "LOW";
export type QuestionImportStatus =
  | "DRAFT"
  | "NEEDS_REVIEW"
  | "VALIDATED"
  | "AUTO_VALIDATED"
  | "PUBLISHED"
  | "INVALID";
export type AutoValidationStatus =
  | "SAFE_TO_AUTO_VALIDATE"
  | "NEEDS_HUMAN_REVIEW"
  | "AUTO_INVALID";
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
  autoValidationScore: number;
  autoValidationStatus: AutoValidationStatus;
  autoValidationWarnings: string | null;
  autoValidationErrors: string | null;
  brokenImageDetected: boolean;
  suspiciousTextDetected: boolean;
  requiresAssetReview: boolean;
}

export interface ReviewQuestionDetail extends Omit<Question, "favorite" | "answered" | "answeredCorrectly"> {
  importedAt: string | null;
  importBatchId: number | null;
  alternativesCount: number;
  assetsCount: number;
  autoValidationScore: number;
  autoValidationStatus: AutoValidationStatus;
  autoValidationErrors: string | null;
  autoValidationWarnings: string | null;
  autoValidatedAt: string | null;
  brokenImageDetected: boolean;
  suspiciousTextDetected: boolean;
  requiresAssetReview: boolean;
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
  autoValidationStatus?: AutoValidationStatus | "";
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

export interface AutoValidationCounters {
  safe: number;
  needsReview: number;
  invalid: number;
  brokenImages: number;
  pendingInep: number;
}

export interface AutoValidationBatchResult {
  processed: number;
  safe: number;
  needsReview: number;
  invalid: number;
  published: number;
}

export interface OfficialExamSource {
  id: number;
  exam: string;
  year: number;
  day: number | null;
  bookColor: string | null;
  pdfUrl: string;
  answerKeyUrl: string | null;
  sourceUrl: string;
  localPdfPath: string | null;
  answerKeyMapJson: string | null;
  createdAt: string;
}

export interface OfficialExamSourcePayload {
  exam: string;
  year: number;
  day?: number | null;
  bookColor?: string | null;
  pdfUrl: string;
  answerKeyUrl?: string | null;
  sourceUrl: string;
  localPdfPath?: string | null;
  answerKeyMapJson?: string | null;
}

export interface OfficialValidationItem {
  questionId: number;
  title: string;
  sourceQuestionNumber: number | null;
  previousValidatedAgainstOfficialSource: boolean | null;
  newValidatedAgainstOfficialSource: boolean | null;
  previousScore: number | null;
  newScore: number | null;
  importStatus: QuestionImportStatus;
  autoValidationStatus: AutoValidationStatus;
  validatedAgainstOfficialSource: boolean;
  assetRecovered: boolean;
  recoveredAssets: number;
  newAutoValidationScore: number | null;
  newAutoValidationStatus: AutoValidationStatus | null;
  requiresAssetReview: boolean;
  brokenImageDetected: boolean;
  recoveryAttempted: boolean;
  officialSourceFound: boolean;
  pdfDownloaded: boolean;
  pdfSizeBytes: number | null;
  pdfPageCount: number | null;
  candidatePages: number[];
  selectedPage: number | null;
  pdfRendered: boolean;
  renderedWidth: number | null;
  renderedHeight: number | null;
  storageUploadAttempted: boolean;
  storageUploadSuccess: boolean;
  recoveryFailureReason: string | null;
  recoveryMethod: string | null;
  assetUrl: string | null;
  updated: boolean;
  warnings: string[];
  errors: string[];
}

export interface OfficialValidationReport {
  totalProcessed: number;
  processed: number;
  validated: number;
  skipped: number;
  failed: number;
  ambiguousOfficialSource: number;
  answerKeyMissing: number;
  answerKeyMismatch: number;
  updatedQuestions: number;
  needsReview: number;
  invalid: number;
  brokenImages: number;
  pendingAssets: number;
  pendingInep: number;
  assetRecovered: number;
  assetRecoveryFailed: number;
  items: OfficialValidationItem[];
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
