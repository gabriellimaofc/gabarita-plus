export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  username: string;
  bio: string | null;
  targetCourse: string | null;
  active: boolean;
  roles: string[];
  createdAt: string;
}

export interface UserStatistics {
  totalAnswers: number;
  totalCorrectAnswers: number;
  accuracyRate: number;
  averageTimeSpentSeconds: number;
  recentAnswers: {
    answerId: number;
    questionId: number;
    questionTitle: string;
    subject: string;
    chosenAlternative: string;
    correct: boolean;
    timeSpentSeconds: number;
    attemptNumber: number;
    answeredAt: string;
  }[];
}

export interface UpdateProfilePayload {
  fullName: string;
  bio: string;
  targetCourse: string;
}
