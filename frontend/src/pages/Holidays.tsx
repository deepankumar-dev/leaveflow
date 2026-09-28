import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useToast } from '../components/Toast'
import { Empty, ErrorBox, PageHeader, Spinner, fmtDate } from '../components/ui'

export default function Holidays() {
  const qc = useQueryClient()
  const toast = useToast()
  const [date, setDate] = useState('')
  const [name, setName] = useState('')
  const { data, isLoading, error } = useQuery({ queryKey: ['holidays'], queryFn: api.holidays })

  const add = useMutation({
    mutationFn: () => api.addHoliday(date, name),
    onSuccess: () => {
      toast.success('Holiday added: working-day counts update automatically')
      setDate('')
      setName('')
      qc.invalidateQueries({ queryKey: ['holidays'] })
    },
    onError: toast.error,
  })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    add.mutate()
  }

  return (
    <>
      <PageHeader title="Holidays" subtitle="Holidays are excluded from working days, in previews and when a request is applied." />
      <div className="grid gap-6 lg:grid-cols-3">
        <form onSubmit={submit} className="card space-y-4 self-start p-6">
          <h2 className="text-sm font-semibold text-slate-700">Add a holiday</h2>
          <div>
            <label className="label" htmlFor="hdate">Date</label>
            <input id="hdate" type="date" className="input" required value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="hname">Name</label>
            <input id="hname" className="input" required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <button className="btn-primary" disabled={add.isPending}>Add</button>
        </form>
        <div className="lg:col-span-2">
          {isLoading && <Spinner />}
          {error && <ErrorBox error={error} />}
          {data?.length === 0 && <Empty title="No holidays configured" />}
          {data && data.length > 0 && (
            <div className="card divide-y divide-slate-100">
              {data.map((h) => (
                <div key={h.id} className="flex justify-between px-5 py-3 text-sm">
                  <span className="font-medium text-slate-800">{h.name}</span>
                  <span className="text-slate-500">{fmtDate(h.date)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  )
}
