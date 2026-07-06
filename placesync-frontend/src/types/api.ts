export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
  correlationId: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
