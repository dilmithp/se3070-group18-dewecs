import { useEffect } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { post, qs } from '../api.js';
import { useAction, useApi, useForm, useOfficer } from '../hooks.jsx';
import { fmtDate, pretty } from '../format.js';
import { Badge, Btn, Card, DL, Field, Filters, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function GroundReportList() {
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useApi('/ground-reports' + qs({
    status: params.get('status'), districtId: params.get('districtId'), category: params.get('category'),
  }));
  return (
    <>
      <PageHeader title="Ground reports" />
      {data && (
        <Filters params={params} setParams={setParams} filters={[
          { key: 'status', label: 'Status', options: data.statuses },
          { key: 'category', label: 'Category', options: data.categories },
          { key: 'districtId', label: 'District', options: data.districts },
        ]} />
      )}
      <Status loading={loading} error={error}>
        <Table rows={data?.reports} empty="No ground reports match these filters." columns={[
          { label: 'ID', render: (r) => <Link to={`/ground-reports/${r.id}`}>#{r.id}</Link> },
          { label: 'Category', render: (r) => pretty(r.category) },
          { label: 'District', render: (r) => r.districtName },
          { label: 'Reporter', render: (r) => r.reporterName },
          { label: 'Status', render: (r) => <Badge value={r.status} /> },
          { label: 'Submitted', render: (r) => fmtDate(r.submittedAt) },
        ]} />
      </Status>
    </>
  );
}

export function GroundReportDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useApi(`/ground-reports/${id}`);
  const [officer, setOfficer] = useOfficer();
  const note = useForm({ note: '' });
  const { run, busy } = useAction(reload);
  const r = data?.report;
  const body = (extra) => ({ reviewingUserId: officer, ...extra });
  useEffect(() => {
    if (data?.users && officer && !data.users.some((u) => String(u.id) === String(officer))) setOfficer('');
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps
  const final = r && ['VERIFIED', 'REJECTED', 'ACTIONED'].includes(r.status);
  return (
    <>
      <PageHeader title={`Ground report #${id}`} />
      <Status loading={loading} error={error}>
        {r && (
          <>
            <Card>
              <DL items={[
                ['Status', <Badge value={r.status} />],
                ['Category', pretty(r.category)],
                ['District', r.districtName],
                ['Reporter', `${r.reporterName ?? '—'}${r.reporterPhone ? ` · ${r.reporterPhone}` : ''}`],
                ['GPS', r.gpsLat != null ? `${r.gpsLat}, ${r.gpsLng}` : '—'],
                ['Submitted', fmtDate(r.submittedAt)],
                ['Reviewed by', r.verifiedByName],
                ['Action note', r.actionNote],
              ]} />
              <h3>Description</h3>
              <p className="pre">{r.description}</p>
              {r.photoUrl && <img className="photo" alt="Reported hazard" src={r.photoUrl.startsWith('http') ? r.photoUrl : `/backend${r.photoUrl}`} />}
            </Card>
            {!final && (
              <Card title="Review">
                <Field label="Reviewing officer">
                  <Select value={officer} onChange={setOfficer} options={data.users} getLabel={(u) => u.fullName} />
                </Field>
                <div className="actions">
                  <Btn disabled={busy || !officer} onClick={() => run(() => post(`/ground-reports/${id}/review`, body()))}>Mark reviewed</Btn>
                  <Btn kind="danger" disabled={busy || !officer} onClick={() => run(() => post(`/ground-reports/${id}/dismiss`, body()), 'Dismiss this report?')}>Dismiss</Btn>
                </div>
                <form className="stack" onSubmit={note.submit(async (v) => {
                  const res = await post(`/ground-reports/${id}/action`, body(v));
                  note.setValues({ note: '' });
                  run(async () => res);
                })}>
                  <Field label="Action note" error={note.errors.note} hint="Record what was done in response.">
                    <textarea rows={3} value={note.values.note} onChange={note.set('note')} />
                  </Field>
                  <div><Btn kind="primary" disabled={note.busy || !officer}>Mark actioned</Btn></div>
                  {note.formError && <div className="alert error">{note.formError}</div>}
                </form>
              </Card>
            )}
          </>
        )}
      </Status>
    </>
  );
}
