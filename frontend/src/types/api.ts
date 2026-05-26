export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  metadata?: unknown;
  timestamp?: string;
}

export interface PageMetadata {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ValidationErrorResponse {
  success: false;
  message: string;
  metadata?: Record<string, string>;
  timestamp?: string;
}
