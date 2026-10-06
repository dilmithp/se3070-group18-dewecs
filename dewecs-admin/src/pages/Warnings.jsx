import { useEffect } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm, useOfficer } from '../hooks.jsx';
import { eventLabel, fmtDate, pretty, toInput } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, LinkBtn, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function WarningList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/warnings' + qs({ status: params.get('status'), districtId: params.get('districtId') }));
  return (
    <>
      <PageHeader title="Warnings"><LinkBtn kind="primary" to="/warnings/new">New warning</LinkBtn></PageHeader>
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'status', label: 'Status', options: data.statuses },
          { key: 'districtId', label: 'District', options: data.districts },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.warnings} empty="No warnings match these filters." columns={[
          { label: 'ID', render: (w) => <Link to={`/warnings/${w.id}`}>#{w.id}</Link> },
          { label: 'Hazard', render: (w) => pretty(w.hazardType) },
          { label: 'District', render: (w) => w.districtName ?? '—' },
          { label: 'Severity', render: (w) => <Badge value={w.severity} /> },
          { label: 'Status', render: (w) => <Badge value={w.status} /> },
          { label: 'Issued', render: (w) => fmtDate(w.issuedAt) },
          { label: 'Expires', render: (w) => fmtDate(w.expiresAt) },
        ]} />
      </Status>
    </>
  );
}

export function WarningDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/warnings/${id}`);
  const { run, busy } = useAction(reload);
  const w = data?.warning;
  return (
    <>
      <PageHeader title={`Warning #${id}`}>
        {w?.status === 'DRAFT' && <LinkBtn to={`/warnings/${id}/edit`}>Edit</LinkBtn>}
        {w?.status === 'DRAFT' && <Btn kind="primary" disabled={busy} onClick={() => run(() => post(`/warnings/${id}/publish`), 'Publish this warning?')}>Publish</Btn>}
        {(w?.status === 'DRAFT' || w?.status === 'ISSUED') && (
          <Btn kind="danger" disabled={busy} onClick={() => run(() => post(`/warnings/${id}/retract`), 'Retract this warning?')}>Retract</Btn>
        )}
      </PageHeader>
      <Status loading={loading} error={error}>
        {w && (
          <Card>
            <DL items={[
              ['Status', <Badge value={w.status} />],
              ['Severity', <Badge value={w.severity} />],
              ['Hazard', pretty(w.hazardType)],
              ['District', w.districtName],
              ['Issued by', w.issuedByName],
              ['Issued at', fmtDate(w.issuedAt)],
              ['Expires at', fmtDate(w.expiresAt)],
              ['Channels', w.broadcastChannels?.map(pretty).join(', ') || '—'],
            ]} />
            <h3>Message</h3>
            <p className="pre">{w.message || '—'}</p>
          </Card>
        )}
      </Status>
    </>
  );
}

export function WarningForm() {
  const { id } = useParams();
  const refs = useApi(id ? `/warnings/${id}/edit` : '/warnings/new');
  const current = useApi(id ? `/warnings/${id}` : null);
  const status = [refs, current].find((r) => r.error || r.loading);
  return (
    <>
      <PageHeader title={id ? `Edit warning #${id}` : 'New warning'} />
      <Status loading={!!status?.loading} error={status?.error}>
        {refs.data && (id ? current.data : true) &&
          (refs.data.hazardEvents ? (
            <WarningFields refs={refs.data} warning={id ? current.data.warning : null} id={id} />
          ) : (
            <div className="alert error">Only drafts can be edited.</div>
          ))}
      </Status>
    </>
  );
}

function WarningFields({ refs, warning, id }) {
  const navigate = useNavigate();
  const [officer, setOfficer] = useOfficer();
  const event = warning && refs.hazardEvents.find((e) => e.hazardType === warning.hazardType && e.district?.id === warning.districtId);
  const user = warning && refs.users.find((u) => u.fullName === warning.issuedByName);
  const f = useForm({
    hazardEventId: event?.id ?? '',
    issuedByUserId: user?.id ?? officer,
    severity: warning?.severity ?? '',
    message: warning?.message ?? '',
    expiresAt: toInput(warning?.expiresAt),
    broadcastChannels: warning?.broadcastChannels ?? [],
  });
  const toggle = (c) =>
    f.setValues((s) => ({
      ...s,
      broadcastChannels: s.broadcastChannels.includes(c) ? s.broadcastChannels.filter((x) => x !== c) : [...s.broadcastChannels, c],
    }));
  useEffect(() => {
    if (f.values.issuedByUserId) setOfficer(String(f.values.issuedByUserId));
  }, [f.values.issuedByUserId]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <form className="card form" onSubmit={f.submit(async (v) => {
      const res = await post(id ? `/warnings/${id}` : '/warnings', {
        ...v,
        expiresAt: v.expiresAt || undefined,
      });
      navigate(res.location);
    })}>
      {f.formError && <div className="alert error">{f.formError}</div>}
      <Field label="Hazard event" error={f.errors.hazardEventId}>
        <Select value={f.values.hazardEventId} onChange={f.set('hazardEventId')} options={refs.hazardEvents} getLabel={eventLabel} />
      </Field>
      <Field label="Issuing officer" error={f.errors.issuedByUserId}>
        <Select value={f.values.issuedByUserId} onChange={f.set('issuedByUserId')} options={refs.users} getLabel={(u) => u.fullName} />
      </Field>
      <Field label="Severity" error={f.errors.severity}>
        <Select value={f.values.severity} onChange={f.set('severity')} options={refs.severities} getValue={(s) => s} getLabel={pretty} />
      </Field>
      <Field label="Message" error={f.errors.message} hint="Required to publish (max 2,000 characters).">
        <textarea rows={5} maxLength={2000} value={f.values.message} onChange={f.set('message')} />
      </Field>
      <Field label="Expires at" error={f.errors.expiresAt}>
        <input type="datetime-local" value={f.values.expiresAt} onChange={f.set('expiresAt')} />
      </Field>
      <fieldset>
        <legend>Broadcast channels</legend>
        <div className="checks">
          {refs.channels.map((c) => (
            <label key={c}>
              <input type="checkbox" checked={f.values.broadcastChannels.includes(c)} onChange={() => toggle(c)} /> {pretty(c)}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="actions">
        <Btn kind="primary" disabled={f.busy}>{id ? 'Save draft' : 'Create draft'}</Btn>
        <LinkBtn to={id ? `/warnings/${id}` : '/warnings'}>Cancel</LinkBtn>
      </div>
    </form>
  );
}
