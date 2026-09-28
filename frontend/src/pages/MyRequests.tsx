import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { RequestTable } from '../components/RequestTable'
import { Empty, ErrorBox, PageHeader, Spinner } from '../components/ui'

export default function MyRequests() {
  const { data, isLoading, error } = useQuery({ queryKey: ['mine'], queryFn: api.mine })
  return (
    <>
      <PageHeader title="My requests" subtitle="Click a request to see its timeline and where it is in the workflow."
        action={<Link to="/apply" className="btn-primary">New request</Link>} />
      {isLoading && <Spinner />}
      {error && <ErrorBox error={error} />}
      {data && data.length === 0 && <Empty title="No requests yet" hint="When you apply for leave, it shows up here with its approval progress." />}
      {data && data.length > 0 && <RequestTable rows={data} showEmployee={false} />}
    </>
  )
}
