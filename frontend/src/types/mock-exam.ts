export interface MockExamQuestion {
  questionId: number;
  questionOrder: number;
  title: string;
  subject: string;
  difficulty: string;
}

export interface MockExam {
  id: number;
  title: string;
  durationMinutes: number;
  finished: boolean;
  finalScore: number | null;
  questions: MockExamQuestion[];
  createdAt: string;
}

export interface CreateMockExamPayload {
  title: string;
  durationMinutes: number;
  questionIds: number[];
}

export interface FinishMockExamPayload {
  finalScore: number;
}
