import { useParams, useNavigate } from 'react-router-dom';
import { AlertTriangle, Lock, ServerCrash, Search } from 'lucide-react';

const ERROR_CONFIG: Record<string, { icon: React.ReactNode; title: string; message: string }> = {
  '401': {
    icon: <Lock size={64} className="text-yellow-400" />,
    title: '인증이 필요합니다',
    message: '이 페이지에 접근하려면 로그인이 필요합니다.',
  },
  '403': {
    icon: <Lock size={64} className="text-red-400" />,
    title: '접근이 거부되었습니다',
    message: '이 페이지에 접근할 권한이 없습니다.',
  },
  '404': {
    icon: <Search size={64} className="text-blue-400" />,
    title: '페이지를 찾을 수 없습니다',
    message: '요청하신 페이지가 존재하지 않거나 삭제되었습니다.',
  },
  '500': {
    icon: <ServerCrash size={64} className="text-red-400" />,
    title: '서버 오류',
    message: '일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
  },
};

const FALLBACK = {
  icon: <AlertTriangle size={64} className="text-yellow-400" />,
  title: '오류가 발생했습니다',
  message: '예기치 않은 오류가 발생했습니다.',
};

export default function ErrorPage() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const config = (code !== undefined && ERROR_CONFIG[code]) || FALLBACK;

  return (
    <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center text-white">
      <div className="text-center px-4">
        <div className="flex justify-center mb-6">{config.icon}</div>
        <h1 className="text-4xl font-bold mb-3">{code ?? 'Error'}</h1>
        <h2 className="text-2xl font-semibold mb-4">{config.title}</h2>
        <p className="text-gray-400 mb-8 max-w-md mx-auto">{config.message}</p>
        <div className="flex gap-4 justify-center">
          <button
            onClick={() => navigate('/')}
            className="px-6 py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition"
          >
            홈으로
          </button>
          <button
            onClick={() => navigate(-1)}
            className="px-6 py-3 bg-gray-700 hover:bg-gray-600 text-white font-semibold rounded-lg transition"
          >
            뒤로가기
          </button>
        </div>
      </div>
    </div>
  );
}
