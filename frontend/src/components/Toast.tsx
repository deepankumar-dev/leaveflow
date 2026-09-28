import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { ApiError } from '../api'

type Kind = 'success' | 'error'
interface ToastItem {
  id: number
  kind: Kind
  text: string
}
interface ToastApi {
  success: (text: string) => void
  error: (e: unknown) => void
}

const Ctx = createContext<ToastApi | null>(null)
let nextId = 1

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const push = useCallback((kind: Kind, text: string) => {
    const id = nextId++
    setItems((xs) => [...xs, { id, kind, text }])
    setTimeout(() => setItems((xs) => xs.filter((x) => x.id !== id)), 5000)
  }, [])

  const api: ToastApi = {
    success: (t) => push('success', t),
    error: (e) => {
      const msg = e instanceof ApiError ? e.message : e instanceof Error ? e.message : 'Something went wrong'
      push('error', msg)
    },
  }

  return (
    <Ctx.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-80 flex-col gap-2" aria-live="polite">
        {items.map((t) => (
          <div
            key={t.id}
            role={t.kind === 'error' ? 'alert' : 'status'}
            className={`pointer-events-auto rounded-lg border px-4 py-3 text-sm shadow-lg ${
              t.kind === 'error'
                ? 'border-rose-200 bg-rose-50 text-rose-800'
                : 'border-emerald-200 bg-emerald-50 text-emerald-800'
            }`}
          >
            {t.text}
          </div>
        ))}
      </div>
    </Ctx.Provider>
  )
}

export function useToast() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useToast outside ToastProvider')
  return v
}
