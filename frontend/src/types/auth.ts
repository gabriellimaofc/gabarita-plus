import type { UserProfile } from "@/types/user";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  username: string;
  password: string;
  targetCourse?: string;
}
