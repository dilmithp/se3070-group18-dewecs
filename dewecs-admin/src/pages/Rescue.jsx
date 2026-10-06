import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm } from '../hooks.jsx';
import { fmtDate } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, LinkBtn, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function RescueList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/rescue-requests' + qs({
    status: params.get('status'), priority: params.get('priority'), districtId: params.get('districtId'),
  }));
  return (
    <>
      <PageHeader title="Rescue requests"><LinkBtn kind="primary" to="/rescue-requests/new">New request</LinkBtn></PageHeader>
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'status', label: 'Status', options: data.statuses },
          { key: 'priority', label: 'Priority', options: data.priorities },
          { key: 'districtId', label: 'District', options: data.districts },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.requests} empty="No rescue requests match these filters." columns={[
          { label: 'ID', render: (r) => <Link to={`/rescue-requests/${r.id}`}>#{r.id}</Link> },
          { label: 'Requester', render: (r) => r.requesterName },
          { label: 'District', render: (r) => r.districtName },
          { label: 'Priority', render: (r) => <Badge value={r.priority} /> },
          { label: 'Status', render: (r) => <Badge value={r.status} /> },
          { label: 'Team', render: (r) => r.assignedTeamName ?? '—' },
          { label: 'Submitted', render: (r) => fmtDate(r.submittedAt) },
        ]} />
      </Status>
    </>
  );
}

export function RescueDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/rescue-requests/${id}`);
  const { run, busy } = useAction(reload);
  const [teamId, setTeamId] = useState('');
  const r = data?.request;
  const active = r && (r.status === 'PENDING' || r.status === 'ASSIGNED');
  return (
    <>
      <PageHeader title={`Rescue request #${id}`}>
        {r?.status === 'ASSIGNED' && <Btn kind="primary" disabled={busy} onClick={() => run(() => post(`/rescue-requests/${id}/complete`), 'Mark as completed?')}>Complete</Btn>}
        {active && <Btn kind="danger" disabled={busy} onClick={() => run(() => post(`/rescue-requests/${id}/cancel`), 'Cancel this request?')}>Cancel request</Btn>}
      </PageHeader>
      <Status loading={loading} error={error}>
        {r && (
          <>
            <Card>
              <DL items={[
                ['Status', <Badge value={r.status} />],
                ['Priority', <Badge value={r.priority} />],
                ['Requester', `${r.requesterName} · ${r.requesterPhone}`],
                ['District', r.districtName],
                ['GPS', r.gpsLat != null ? `${r.gpsLat}, ${r.gpsLng}` : '—'],
                ['Assigned team', r.assignedTeamName],
                ['Submitted', fmtDate(r.submittedAt)],
                ['Assigned', fmtDate(r.assignedAt)],
                ['Completed', fmtDate(r.completedAt)],
              ]} />
              <h3>Situation</h3>
              <p className="pre">{r.description}</p>
            </Card>
            {r.status === 'PENDING' && (
              <Card title="Assign a team">
                <div className="inline-form">
                  <Field label="Available team">
                    <Select value={teamId} onChange={setTeamId} options={data.availableTeams}
                      getLabel={(t) => `${t.name}${t.district ? ` (${t.district.name})` : ''}`} />
                  </Field>
                  <Btn kind="primary" disabled={busy || !teamId} onClick={() => run(() => post(`/rescue-requests/${id}/assign`, { teamId }))}>Assign</Btn>
                </div>
              </Card>
            )}
          </>
        )}
      </Status>
    </>
  );
}

export function RescueForm() {
  const refs = useApi('/rescue-requests/new');
  return (
    <>
      <PageHeader title="New rescue request" />
      <Status loading={refs.loading} error={refs.error}>{refs.data && <RescueFields refs={refs.data} />}</Status>
    </>
  );
}

function RescueFields({ refs }) {
  const navigate = useNavigate();
  const f = useForm({ districtId: '', requesterName: '', requesterPhone: '', gpsLat: '', gpsLng: '', description: '', priority: '' });
  return (
    <form className="card form" onSubmit={f.submit(async (v) => navigate((await post('/rescue-requests', v)).location))}>
      {f.formError && <div className="alert error">{f.formError}</div>}
      <Field label="Requester name" error={f.errors.requesterName}><input value={f.values.requesterName} onChange={f.set('requesterName')} /></Field>
      <Field label="Contact phone" error={f.errors.requesterPhone}><input value={f.values.requesterPhone} onChange={f.set('requesterPhone')} /></Field>
      <Field label="District" error={f.errors.districtId}><Select value={f.values.districtId} onChange={f.set('districtId')} options={refs.districts} /></Field>
      <Field label="Priority" error={f.errors.priority}>
        <Select value={f.values.priority} onChange={f.set('priority')} options={refs.priorities} getValue={(p) => p} getLabel={(p) => p} />
      </Field>
      <div className="row">
        <Field label="Latitude" error={f.errors.gpsLat}><input type="number" step="any" value={f.values.gpsLat} onChange={f.set('gpsLat')} /></Field>
        <Field label="Longitude" error={f.errors.gpsLng}><input type="number" step="any" value={f.values.gpsLng} onChange={f.set('gpsLng')} /></Field>
      </div>
      <Field label="Situation" error={f.errors.description}><textarea rows={4} value={f.values.description} onChange={f.set('description')} /></Field>
      <div className="actions">
        <Btn kind="primary" disabled={f.busy}>Submit request</Btn>
        <LinkBtn to="/rescue-requests">Cancel</LinkBtn>
      </div>
    </form>
  );
}
