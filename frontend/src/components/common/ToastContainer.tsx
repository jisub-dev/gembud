import { CheckCircle, XCircle, Info, X } from 'lucide-react';
import { useToastStore } from '@/hooks/useToast';

export function ToastContainer() {
  const { toasts, removeToast } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`
            flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg
            min-w-[260px] max-w-[380px] animate-fade-in
            ${toast.type === 'success' ? 'bg-green-700 text-white' : ''}
            ${toast.type === 'error' ? 'bg-red-700 text-white' : ''}
            ${toast.type === 'info' ? 'bg-gray-700 text-white' : ''}
          `}
        >
          <span className="flex-shrink-0">
            {toast.type === 'success' && <CheckCircle size={18} />}
            {toast.type === 'error' && <XCircle size={18} />}
            {toast.type === 'info' && <Info size={18} />}
          </span>
          <span className="text-sm font-medium flex-1">{toast.message}</span>
          <button
            onClick={() => removeToast(toast.id)}
            className="flex-shrink-0 opacity-70 hover:opacity-100 transition-opacity"
          >
            <X size={15} />
          </button>
        </div>
      ))}
    </div>
  );
}
