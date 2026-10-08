import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm } from '../hooks.jsx';
import { fmtDate } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, LinkBtn, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function DistributionList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/relief-distributions' + qs({
    status: params.get('status'), shelterId: params.get('shelterId'), resourceId: params.get('resourceId'),
  }));
  return (
    <>
      <PageHeader title="Relief distributions"><LinkBtn kind="primary" to="/relief-distributions/new">New distribution</LinkBtn></PageHeader>
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'status', label: 'Status', options: data.statuses },
          { key: 'shelterId', label: 'Shelter', options: data.shelters },
          { key: 'resourceId', label: 'Supply', options: data.supplies },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.distributions} empty="No distributions match these filters." columns={[
          { label: 'ID', render: (d) => <Link to={`/relief-distributions/${d.id}`}>#{d.id}</Link> },
          { label: 'Supply', render: (d) => d.resourceName },
          { label: 'Quantity', render: (d) => `${d.quantity} ${d.resourceUnit}` },
          { label: 'Shelter', render: (d) => d.shelterName },
          { label: 'Status', render: (d) => <Badge value={d.status} /> },
          { label: 'Dispatched', render: (d) => fmtDate(d.dispatchedAt) },
        ]} />
      </Status>
    </>
  );
}

export function DistributionDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/relief-distributions/${id}`);
  const { run, busy } = useAction(reload);
  const d = data?.distribution;
  const open = d && !['DELIVERED', 'CANCELLED'].includes(d.status);
  return (
    <>
      <PageHeader title={`Distribution #${id}`}>
        {open && <Btn kind="primary" disabled={busy} onClick={() => run(() => post(`/relief-distributions/${id}/deliver`), 'Mark as delivered?')}>Mark delivered</Btn>}
        {open && <Btn kind="danger" disabled={busy} onClick={() => run(() => post(`/relief-distributions/${id}/cancel`), 'Cancel? Stock will be restored.')}>Cancel</Btn>}
      </PageHeader>
      <Status loading={loading} error={error}>
        {d && (
          <Card>
            <DL items={[
              ['Status', <Badge value={d.status} />],
              ['Supply', d.resourceName],
              ['Quantity', `${d.quantity} ${d.resourceUnit}`],
              ['Destination', <Link to={`/shelters/${d.shelterId}`}>{d.shelterName}</Link>],
              ['Organization', d.organizationName],
              ['Dispatched', fmtDate(d.dispatchedAt)],
              ['Delivered', fmtDate(d.deliveredAt)],
            ]} />
          </Card>
        )}
      </Status>
    </>
  );
}

export function DistributionForm() {
  const refs = useApi('/relief-distributions/new');
  return (
    <>
      <PageHeader title="New relief distribution" />
      <Status loading={refs.loading} error={refs.error}>{refs.data && <DistributionFields refs={refs.data} />}</Status>
    </>
  );
}

function DistributionFields({ refs }) {
  const navigate = useNavigate();
  const f = useForm({ resourceId: '', shelterId: '', quantity: '' });
  const supply = refs.supplies.find((s) => String(s.id) === String(f.values.resourceId));
  return (
    <form className="card form" onSubmit={f.submit(async (v) => navigate((await post('/relief-distributions', v)).location))}>
      {f.formError && <div className="alert error">{f.formError}</div>}
      <Field label="Relief supply" error={f.errors.resourceId}>
        <Select value={f.values.resourceId} onChange={f.set('resourceId')} options={refs.supplies}
          getLabel={(s) => `${s.name} — ${s.quantity} ${s.unit} in stock`} />
      </Field>
      <Field label="Destination shelter" error={f.errors.shelterId}>
        <Select value={f.values.shelterId} onChange={f.set('shelterId')} options={refs.shelters} />
      </Field>
      <Field label={`Quantity${supply ? ` (${supply.unit})` : ''}`} error={f.errors.quantity}>
        <input type="number" min="1" value={f.values.quantity} onChange={f.set('quantity')} />
      </Field>
      <div className="actions">
        <Btn kind="primary" disabled={f.busy}>Dispatch</Btn>
        <LinkBtn to="/relief-distributions">Cancel</LinkBtn>
      </div>
    </form>
  );
}
