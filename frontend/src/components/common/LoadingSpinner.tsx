interface LoadingSpinnerProps {
  label?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const spinnerSizeClass = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-12 w-12 border-[3px]',
};

export default function LoadingSpinner({
  label = '불러오는 중...',
  size = 'md',
  className = '',
}: LoadingSpinnerProps) {
  return (
    <div className={`flex flex-col items-center justify-center gap-3 ${className}`}>
      <div
        className={`${spinnerSizeClass[size]} rounded-full border-purple-500/70 border-t-transparent animate-spin`}
      />
      <p className="text-sm text-gray-400">{label}</p>
    </div>
  );
}

