import { Link } from 'react-router-dom';
import { pretty } from '../format.js';

export function PageHeader({ title, children }) {
  return (
    <div className="page-header">
      <h1>{title}</h1>
      <div className="actions">{children}</div>
    </div>
  );
}

export function Badge({ value }) {
  return <span className={`badge b-${String(value).toLowerCase()}`}>{pretty(value)}</span>;
}

export function Status({ loading, error, children }) {
  if (loading) return <p className="muted">Loading…</p>;
  if (error) return <div className="alert error">{error.message}</div>;
  return children;
}

export function Table({ rows, columns, empty = 'Nothing to show.' }) {
  if (!rows?.length) return <p className="empty">{empty}</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c.label}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id ?? i}>{columns.map((c) => <td key={c.label}>{c.render(r)}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Field({ label, error, children, hint }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small className="muted">{hint}</small>}
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}

export function Select({ value, onChange, options, placeholder = 'Select…', getValue = (o) => o.id, getLabel = (o) => o.name, ...rest }) {
  return (
    <select value={value ?? ''} onChange={(e) => onChange(e.target.value)} {...rest}>
      <option value="">{placeholder}</option>
      {options?.map((o) => (
        <option key={getValue(o)} value={getValue(o)}>{getLabel(o)}</option>
      ))}
    </select>
  );
}

/** A filter bar: each filter is { key, label, options, getValue?, getLabel? }; state lives in the URL. */
export function Filters({ filters, params, setParams }) {
  const set = (key, v) => {
    const next = new URLSearchParams(params);
    if (v) next.set(key, v);
    else next.delete(key);
    setParams(next, { replace: true });
  };
  return (
    <div className="filters">
      {filters.map((f) => (
        <label key={f.key}>
          <span>{f.label}</span>
          {f.checkbox ? (
            <input type="checkbox" checked={params.get(f.key) === 'true'} onChange={(e) => set(f.key, e.target.checked ? 'true' : '')} />
          ) : (
            <Select
              value={params.get(f.key) ?? ''}
              onChange={(v) => set(f.key, v)}
              options={f.options}
              placeholder="All"
              getValue={f.getValue ?? ((o) => o.id ?? o)}
              getLabel={f.getLabel ?? ((o) => o.name ?? pretty(o))}
            />
          )}
        </label>
      ))}
    </div>
  );
}

export function Card({ title, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {title && <h2>{title}</h2>}
      {children}
    </section>
  );
}

export function DL({ items }) {
  return (
    <dl className="dl">
      {items.map(([k, v]) => (
        <div key={k}>
          <dt>{k}</dt>
          <dd>{v ?? '—'}</dd>
        </div>
      ))}
    </dl>
  );
}

export const Btn = ({ kind = '', ...p }) => <button className={`btn ${kind}`} {...p} />;
export const LinkBtn = ({ kind = '', ...p }) => <Link className={`btn ${kind}`} {...p} />;

export function Meter({ value, max }) {
  const pct = max ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className="meter" title={`${value} / ${max}`}>
      <div style={{ width: `${pct}%` }} className={pct >= 100 ? 'full' : pct >= 80 ? 'warn' : ''} />
    </div>
  );
}
