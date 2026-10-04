export const fmtDate = (v) => (v ? new Date(v).toLocaleString() : '—');
export const toInput = (v) => (v ? String(v).slice(0, 16) : '');
export const pretty = (v) => (v ? String(v).replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()) : '—');
export const eventLabel = (e) =>
  `${pretty(e.hazardType)} · ${e.district?.name ?? 'No district'} · ${pretty(e.severityLevel)} · ${fmtDate(e.occurredAt)}`;
export const nameOf = (x) => x?.name ?? x?.fullName ?? `#${x?.id}`;
