export interface PerformanceByDimension {
  name: string;
  total: number;
  correct: number;
  accuracy: number;
}

export interface WeeklyProgress {
  week: string;
  totalAnswers: number;
  correctAnswers: number;
  accuracy: number;
}

export interface DashboardData {
  accuracyRate: number;
  totalAnswered: number;
  averageTimeSpentSeconds: number;
  performanceBySubject: PerformanceByDimension[];
  performanceByTopic: PerformanceByDimension[];
  weeklyProgress: WeeklyProgress[];
}
