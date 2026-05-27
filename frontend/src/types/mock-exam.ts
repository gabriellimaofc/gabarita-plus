import type { Alternative, DifficultyLevel } from "@/types/question";

export interface MockExamQuestion {
  questionId: number;
  questionOrder: number;
  title: string;
  subject: string;
  difficulty: DifficultyLevel;
  answered: boolean;
  correct: boolean | null;
}

export interface MockExam {
  id: number;
  title: string;
  durationMinutes: number;
  finished: boolean;
  finalScore: number | null;
  questionCount: number;
  answeredCount: number;
  correctCount: number;
  incorrectCount: number;
  unansweredCount: number;
  timeSpentSeconds: number | null;
  finishedAt: string | null;
  questions: MockExamQuestion[];
  createdAt: string;
}

export interface MockExamQuestionDetail {
  questionId: number;
  questionOrder: number;
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
  chosenAlternative: string | null;
  answered: boolean | null;
  correct: boolean | null;
  correctAlternative: string | null;
  explanation: string | null;
  alternatives: Alternative[];
}

export interface CreateMockExamPayload {
  title: string;
  durationMinutes: number;
  questionIds?: number[];
  questionCount?: number;
}

export interface SaveMockExamAnswerPayload {
  questionId: number;
  chosenAlternative: string;
  timeSpentSeconds: number;
}

export interface MockExamAnswerResult {
  questionId: number;
  questionOrder: number;
  chosenAlternative: string;
  answeredCount: number;
  unansweredCount: number;
  answeredAt: string;
}

export interface FinishMockExamPayload {
  timeSpentSeconds?: number;
}

export interface MockExamSubjectPerformance {
  subject: string;
  totalQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracy: number;
}

export interface MockExamResult {
  id: number;
  title: string;
  finished: boolean;
  finalScore: number;
  questionCount: number;
  correctAnswers: number;
  incorrectAnswers: number;
  unansweredQuestions: number;
  timeSpentSeconds: number | null;
  finishedAt: string | null;
  performanceBySubject: MockExamSubjectPerformance[];
  questions: MockExamQuestionDetail[];
}
