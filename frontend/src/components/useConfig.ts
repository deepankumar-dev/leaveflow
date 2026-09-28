import { useQuery } from '@tanstack/react-query'
import { api } from '../api'

/** Public server settings; `demoMode` is only true when the backend runs with the demo profile. */
export function useConfig() {
  const { data } = useQuery({ queryKey: ['config'], queryFn: api.config, staleTime: Infinity })
  return { demoMode: data?.demoMode ?? false, loaded: !!data }
}
