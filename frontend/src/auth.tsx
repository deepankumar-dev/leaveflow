import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, getToken, setToken, setUnauthorizedHandler, type User } from './api'

interface AuthState {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => void
  /** Adopt a fresh token and user (after changing the password). */
  setSession: (token: string, user: User) => void
}

const Ctx = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState<boolean>(!!getToken())

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
    qc.clear()
  }, [qc])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!getToken()) return
    api
      .me()
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.login(email, password)
      qc.clear()
      setToken(res.token)
      setUser(res.user)
      return res.user
    },
    [qc],
  )

  const setSession = useCallback((token: string, u: User) => {
    setToken(token)
    setUser(u)
  }, [])

  return <Ctx.Provider value={{ user, loading, login, logout, setSession }}>{children}</Ctx.Provider>
}

export function useAuth() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useAuth outside AuthProvider')
  return v
}
