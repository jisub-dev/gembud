import LoadingSpinner from './LoadingSpinner';

export default function RouteLoadingFallback() {
  return (
    <div className="min-h-screen bg-dark-primary flex items-center justify-center px-4">
      <div className="flex flex-col items-center gap-3 text-center">
        <LoadingSpinner />
        <p className="text-sm text-text-secondary">페이지를 불러오는 중입니다...</p>
      </div>
    </div>
  );
}
