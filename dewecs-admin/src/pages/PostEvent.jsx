import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { post } from '../api.js';
import { useAction, useApi } from '../hooks.jsx';
import { eventLabel, fmtDate, pretty } from '../format.js';
import { Badge, Btn, Card, DL, Field, PageHeader, Select, Status, Table } from '../components/ui.jsx';

export function PostEventList() {
  const { data, error, loading } = useApi('/post-event-reports');
  const navigate = useNavigate();
  const [eventId, setEventId] = useState('');
  const { run, busy } = useAction((res) => res?.location && navigate(res.location));
  return (
    <>
      <PageHeader title="Post-event reports" />
      <Status loading={loading} error={error}>
        <Card title="Generate a report">
          <div className="inline-form">
            <Field label="Hazard event">
              <Select value={eventId} onChange={setEventId} options={data?.events} getLabel={eventLabel} />
            </Field>
            <Btn kind="primary" disabled={busy || !eventId} onClick={() => run(() => post('/post-event-reports', { hazardEventId: eventId }))}>Generate</Btn>
          </div>
        </Card>
        <Table rows={data?.reports} empty="No reports generated yet." columns={[
          { label: 'ID', render: (r) => <Link to={`/post-event-reports/${r.id}`}>#{r.id}</Link> },
          { label: 'Hazard', render: (r) => pretty(r.hazardType) },
          { label: 'District', render: (r) => r.districtName },
          { label: 'Severity', render: (r) => <Badge value={r.severity} /> },
          { label: 'Event status', render: (r) => <Badge value={r.eventStatus} /> },
          { label: 'Occurred', render: (r) => fmtDate(r.occurredAt) },
          { label: 'Generated', render: (r) => fmtDate(r.generatedAt) },
        ]} />
      </Status>
    </>
  );
}

export function PostEventDetail() {
  const { id } = useParams();
  const { data, error, loading } = useApi(`/post-event-reports/${id}`);
  const r = data?.report;
  return (
    <>
      <PageHeader title={`Post-event report #${id}`}><Btn onClick={() => window.print()}>Print</Btn></PageHeader>
      <Status loading={loading} error={error}>
        {r && (
          <>
            <Card>
              <DL items={[
                ['Hazard', pretty(r.hazardType)],
                ['District', r.districtName],
                ['Severity', <Badge value={r.severity} />],
                ['Event status', <Badge value={r.eventStatus} />],
                ['Occurred', fmtDate(r.occurredAt)],
                ['Generated', fmtDate(r.generatedAt)],
              ]} />
            </Card>
            <div className="tiles">
              {r.metrics?.map((m) => (
                <div key={m.title} className="tile info" title={m.explanation}>
                  <strong>{m.value}</strong>
                  <span>{m.title}</span>
                  <small>{m.explanation}</small>
                </div>
              ))}
            </div>
            <Card title="Warnings"><ul>{r.warningLines?.length ? r.warningLines.map((l) => <li key={l}>{l}</li>) : <li className="muted">None</li>}</ul></Card>
            <Card title="Shelters"><ul>{r.shelterLines?.length ? r.shelterLines.map((l) => <li key={l}>{l}</li>) : <li className="muted">None</li>}</ul></Card>
          </>
        )}
      </Status>
    </>
  );
}
