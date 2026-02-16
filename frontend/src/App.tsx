import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-foreground mb-4">
          Gembud
        </h1>
        <p className="text-xl text-muted-foreground mb-8">
          게임 파티원 모집 플랫폼
        </p>
        <button
          onClick={() => setCount((count) => count + 1)}
          className="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
        >
          count is {count}
        </button>
        <p className="mt-4 text-sm text-muted-foreground">
          Phase 0: 프론트엔드 초기 설정 완료
        </p>
      </div>
    </div>
  )
}

export default App
