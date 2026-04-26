import { AxiosError } from 'axios';

export function getApiErrorMessage(e: unknown, fallback = '오류가 발생했습니다'): string {
  if (e instanceof AxiosError) {
    return (e.response?.data as { message?: string })?.message ?? e.message ?? fallback;
  }
  if (e instanceof Error) return e.message;
  return fallback;
}
