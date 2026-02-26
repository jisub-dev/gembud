/**
 * Standard API response wrapper for all successful responses
 */
export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
}
