import { Link } from 'react-router-dom';
import { useApi } from '../hooks.jsx';
import { PageHeader, Status } from '../components/ui.jsx';

const TILES = [
  ['openWarnings', 'Open warnings', '/warnings?status=ISSUED', 'danger'],
  ['fullShelters', 'Full shelters', '/shelters?status=FULL', 'warn'],
  ['pendingRescueRequests', 'Pending rescue requests', '/rescue-requests?status=PENDING', 'danger'],
  ['lowStockSupplies', 'Low-stock supplies', '/relief-supplies?lowStockOnly=true', 'warn'],
  ['unreviewedReports', 'Unreviewed ground reports', '/ground-reports?status=PENDING_REVIEW', 'info'],
];

export default function Dashboard() {
  const { data, error, loading, reload } = useApi('/dashboard');
  const s = data?.summary;
  return (
    <>
      <PageHeader title="Dashboard"><button className="btn" onClick={reload}>Refresh</button></PageHeader>
      <Status loading={loading} error={error}>
        <div className="tiles">
          {TILES.map(([key, label, to, tone]) => (
            <Link key={key} to={to} className={`tile ${tone}`}>
              <strong>{s?.[key] ?? 0}</strong>
              <span>{label}</span>
            </Link>
          ))}
        </div>
      </Status>
    </>
  );
}
