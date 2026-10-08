import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm } from '../hooks.jsx';
import { fmtDate } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, LinkBtn, Meter, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function ShelterList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/shelters' + qs({ status: params.get('status'), districtId: params.get('districtId') }));
  return (
    <>
      <PageHeader title="Shelters"><LinkBtn kind="primary" to="/shelters/new">New shelter</LinkBtn></PageHeader>
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'status', label: 'Status', options: data.statuses },
          { key: 'districtId', label: 'District', options: data.districts },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.shelters} empty="No shelters match these filters." columns={[
          { label: 'Name', render: (s) => <Link to={`/shelters/${s.id}`}>{s.name}</Link> },
          { label: 'District', render: (s) => s.districtName },
          { label: 'Organization', render: (s) => s.organizationName },
          { label: 'Occupancy', render: (s) => <><Meter value={s.currentOccupancy} max={s.capacity} /> {s.currentOccupancy} / {s.capacity}</> },
          { label: 'Status', render: (s) => <Badge value={s.status} /> },
        ]} />
      </Status>
    </>
  );
}

export function ShelterDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/shelters/${id}`);
  const { run, busy } = useAction(reload);
  const s = data?.shelter;
  const f = useForm({ fullName: '', nic: '' });
  return (
    <>
      <PageHeader title={s?.name ?? `Shelter #${id}`}>
        <LinkBtn to={`/shelters/${id}/edit`}>Edit</LinkBtn>
        {s?.status === 'CLOSED' ? (
          <Btn disabled={busy} onClick={() => run(() => post(`/shelters/${id}/reopen`))}>Reopen</Btn>
        ) : (
          <Btn kind="danger" disabled={busy} onClick={() => run(() => post(`/shelters/${id}/close`), 'Close this shelter?')}>Close</Btn>
        )}
      </PageHeader>
      <Status loading={loading} error={error}>
        {s && (
          <>
            <Card>
              <DL items={[
                ['Status', <Badge value={s.status} />],
                ['District', s.districtName],
                ['Organization', s.organizationName],
                ['Occupancy', <><Meter value={s.currentOccupancy} max={s.capacity} /> {s.currentOccupancy} / {s.capacity}</>],
              ]} />
            </Card>
            <Card title="Check in an occupant">
              <form className="inline-form" onSubmit={f.submit(async (v) => {
                const res = await post(`/shelters/${id}/check-in`, v);
                f.setValues({ fullName: '', nic: '' });
                run(async () => res);
              })}>
                <Field label="Full name" error={f.errors.fullName}><input value={f.values.fullName} onChange={f.set('fullName')} /></Field>
                <Field label="NIC" error={f.errors.nic}><input value={f.values.nic} onChange={f.set('nic')} /></Field>
                <Btn kind="primary" disabled={f.busy}>Check in</Btn>
              </form>
              {f.formError && <div className="alert error">{f.formError}</div>}
            </Card>
            <Card title={`Current occupants (${data.occupants?.length ?? 0})`}>
              <Table rows={data.occupants} empty="No one is checked in." columns={[
                { label: 'Name', render: (o) => o.fullName },
                { label: 'NIC', render: (o) => o.nic },
                { label: 'Checked in', render: (o) => fmtDate(o.checkInTime) },
                { label: '', render: (o) => (
                  <Btn disabled={busy} onClick={() => run(() => post(`/shelters/${id}/check-out/${o.id}`), `Check out ${o.fullName}?`)}>Check out</Btn>
                ) },
              ]} />
            </Card>
          </>
        )}
      </Status>
    </>
  );
}

export function ShelterForm() {
  const { id } = useParams();
  const refs = useApi(id ? `/shelters/${id}/edit` : '/shelters/new');
  const current = useApi(id ? `/shelters/${id}` : null);
  const bad = [refs, current].find((r) => r.error || r.loading);
  return (
    <>
      <PageHeader title={id ? `Edit shelter #${id}` : 'New shelter'} />
      <Status loading={!!bad?.loading} error={bad?.error}>
        {refs.data && (!id || current.data) && <ShelterFields refs={refs.data} shelter={current.data?.shelter} id={id} />}
      </Status>
    </>
  );
}

function ShelterFields({ refs, shelter, id }) {
  const navigate = useNavigate();
  const f = useForm({
    districtId: shelter?.districtId ?? '',
    organizationId: shelter ? refs.organizations.find((o) => o.name === shelter.organizationName)?.id ?? '' : '',
    name: shelter?.name ?? '',
    capacity: shelter?.capacity ?? '',
  });
  return (
    <form className="card form" onSubmit={f.submit(async (v) => navigate((await post(id ? `/shelters/${id}` : '/shelters', v)).location))}>
      {f.formError && <div className="alert error">{f.formError}</div>}
      <Field label="Name" error={f.errors.name}><input value={f.values.name} onChange={f.set('name')} /></Field>
      <Field label="District" error={f.errors.districtId}><Select value={f.values.districtId} onChange={f.set('districtId')} options={refs.districts} /></Field>
      <Field label="Owning organization" error={f.errors.organizationId}><Select value={f.values.organizationId} onChange={f.set('organizationId')} options={refs.organizations} /></Field>
      <Field label="Capacity" error={f.errors.capacity}><input type="number" min="1" value={f.values.capacity} onChange={f.set('capacity')} /></Field>
      <div className="actions">
        <Btn kind="primary" disabled={f.busy}>{id ? 'Save' : 'Create shelter'}</Btn>
        <LinkBtn to={id ? `/shelters/${id}` : '/shelters'}>Cancel</LinkBtn>
      </div>
    </form>
  );
}
