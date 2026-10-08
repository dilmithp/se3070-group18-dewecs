import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm } from '../hooks.jsx';
import { pretty } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, LinkBtn, PageHeader, Select, Status, Table } from '../components/ui.jsx';

const stockBadge = (s) => (s.outOfStock ? <Badge value="OUT_OF_STOCK" /> : s.lowStock ? <Badge value="LOW_STOCK" /> : <Badge value="OK" />);

export function SupplyList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/relief-supplies' + qs({
    type: params.get('type'), districtId: params.get('districtId'), lowStockOnly: params.get('lowStockOnly'),
  }));
  return (
    <>
      <PageHeader title="Relief supplies"><LinkBtn kind="primary" to="/relief-supplies/new">New supply</LinkBtn></PageHeader>
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'type', label: 'Category', options: data.types },
          { key: 'districtId', label: 'District', options: data.districts },
          { key: 'lowStockOnly', label: 'Low stock only', checkbox: true },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.supplies} empty="No supplies match these filters." columns={[
          { label: 'Name', render: (s) => <Link to={`/relief-supplies/${s.id}`}>{s.name}</Link> },
          { label: 'Category', render: (s) => pretty(s.type) },
          { label: 'District', render: (s) => s.districtName },
          { label: 'Owner', render: (s) => s.organizationName },
          { label: 'Quantity', render: (s) => `${s.quantity} ${s.unit}` },
          { label: 'Stock', render: stockBadge },
        ]} />
      </Status>
    </>
  );
}

export function SupplyDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/relief-supplies/${id}`);
  const f = useForm({ quantity: '' });
  const { run } = useAction(reload);
  const s = data?.supply;
  return (
    <>
      <PageHeader title={s?.name ?? `Supply #${id}`}><LinkBtn to={`/relief-supplies/${id}/edit`}>Edit</LinkBtn></PageHeader>
      <Status loading={loading} error={error}>
        {s && (
          <>
            <Card>
              <DL items={[
                ['Stock', stockBadge(s)],
                ['Quantity', `${s.quantity} ${s.unit}`],
                ['Category', pretty(s.type)],
                ['District', s.districtName],
                ['Owner', s.organizationName],
              ]} />
            </Card>
            <Card title="Restock">
              <form className="inline-form" onSubmit={f.submit(async (v) => {
                const res = await post(`/relief-supplies/${id}/restock`, v);
                f.setValues({ quantity: '' });
                run(async () => res);
              })}>
                <Field label={`Quantity to add (${s.unit})`} error={f.errors.quantity}>
                  <input type="number" min="1" value={f.values.quantity} onChange={f.set('quantity')} />
                </Field>
                <Btn kind="primary" disabled={f.busy}>Restock</Btn>
              </form>
              {f.formError && <div className="alert error">{f.formError}</div>}
            </Card>
          </>
        )}
      </Status>
    </>
  );
}

export function SupplyForm() {
  const { id } = useParams();
  const refs = useApi(id ? `/relief-supplies/${id}/edit` : '/relief-supplies/new');
  const current = useApi(id ? `/relief-supplies/${id}` : null);
  const bad = [refs, current].find((r) => r.error || r.loading);
  return (
    <>
      <PageHeader title={id ? `Edit supply #${id}` : 'New relief supply'} />
      <Status loading={!!bad?.loading} error={bad?.error}>
        {refs.data && (!id || current.data) && <SupplyFields refs={refs.data} supply={current.data?.supply} id={id} />}
      </Status>
    </>
  );
}

function SupplyFields({ refs, supply, id }) {
  const navigate = useNavigate();
  const f = useForm({
    districtId: supply?.districtId ?? '',
    organizationId: supply ? refs.organizations.find((o) => o.name === supply.organizationName)?.id ?? '' : '',
    name: supply?.name ?? '',
    type: supply?.type ?? '',
    unit: supply?.unit ?? '',
    quantity: supply?.quantity ?? '',
  });
  return (
    <form className="card form" onSubmit={f.submit(async (v) => navigate((await post(id ? `/relief-supplies/${id}` : '/relief-supplies', v)).location))}>
      {f.formError && <div className="alert error">{f.formError}</div>}
      <Field label="Name" error={f.errors.name}><input value={f.values.name} onChange={f.set('name')} /></Field>
      <Field label="Category" error={f.errors.type}>
        <Select value={f.values.type} onChange={f.set('type')} options={refs.types} getValue={(t) => t} getLabel={pretty} />
      </Field>
      <div className="row">
        <Field label="Unit" error={f.errors.unit}><input placeholder="kg, boxes…" value={f.values.unit} onChange={f.set('unit')} /></Field>
        <Field label="Quantity" error={f.errors.quantity}><input type="number" min="0" value={f.values.quantity} onChange={f.set('quantity')} /></Field>
      </div>
      <Field label="District" error={f.errors.districtId}><Select value={f.values.districtId} onChange={f.set('districtId')} options={refs.districts} /></Field>
      <Field label="Owning organization" error={f.errors.organizationId}><Select value={f.values.organizationId} onChange={f.set('organizationId')} options={refs.organizations} /></Field>
      <div className="actions">
        <Btn kind="primary" disabled={f.busy}>{id ? 'Save' : 'Create supply'}</Btn>
        <LinkBtn to={id ? `/relief-supplies/${id}` : '/relief-supplies'}>Cancel</LinkBtn>
      </div>
    </form>
  );
}
