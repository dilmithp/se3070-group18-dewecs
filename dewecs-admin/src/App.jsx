import { useCallback, useState } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import { ToastProvider } from './hooks.jsx';
import Dashboard from './pages/Dashboard.jsx';
import { WarningList, WarningDetail, WarningForm } from './pages/Warnings.jsx';
import { ShelterList, ShelterDetail, ShelterForm } from './pages/Shelters.jsx';
import { RescueList, RescueDetail, RescueForm } from './pages/Rescue.jsx';
import { SupplyList, SupplyDetail, SupplyForm } from './pages/Supplies.jsx';
import { DistributionList, DistributionDetail, DistributionForm } from './pages/Distributions.jsx';
import { GroundReportList, GroundReportDetail } from './pages/GroundReports.jsx';
import { PostEventList, PostEventDetail } from './pages/PostEvent.jsx';

const NAV = [
  ['/', 'Dashboard'],
  ['/warnings', 'Warnings'],
  ['/shelters', 'Shelters'],
  ['/rescue-requests', 'Rescue requests'],
  ['/relief-supplies', 'Relief supplies'],
  ['/relief-distributions', 'Relief distributions'],
  ['/ground-reports', 'Ground reports'],
  ['/post-event-reports', 'Post-event reports'],
];

export default function App() {
  const [toasts, setToasts] = useState([]);
  const toast = useCallback((text, kind = 'ok') => {
    const id = Math.random();
    setToasts((t) => [...t, { id, text, kind }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 5000);
  }, []);

  return (
    <ToastProvider value={toast}>
      <div className="layout">
        <aside className="sidebar">
          <div className="brand">DEWECS<small>Officer console</small></div>
          <nav>
            {NAV.map(([to, label]) => (
              <NavLink key={to} to={to} end={to === '/'}>{label}</NavLink>
            ))}
          </nav>
        </aside>
        <main className="content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/warnings" element={<WarningList />} />
            <Route path="/warnings/new" element={<WarningForm />} />
            <Route path="/warnings/:id" element={<WarningDetail />} />
            <Route path="/warnings/:id/edit" element={<WarningForm />} />
            <Route path="/shelters" element={<ShelterList />} />
            <Route path="/shelters/new" element={<ShelterForm />} />
            <Route path="/shelters/:id" element={<ShelterDetail />} />
            <Route path="/shelters/:id/edit" element={<ShelterForm />} />
            <Route path="/rescue-requests" element={<RescueList />} />
            <Route path="/rescue-requests/new" element={<RescueForm />} />
            <Route path="/rescue-requests/:id" element={<RescueDetail />} />
            <Route path="/relief-supplies" element={<SupplyList />} />
            <Route path="/relief-supplies/new" element={<SupplyForm />} />
            <Route path="/relief-supplies/:id" element={<SupplyDetail />} />
            <Route path="/relief-supplies/:id/edit" element={<SupplyForm />} />
            <Route path="/relief-distributions" element={<DistributionList />} />
            <Route path="/relief-distributions/new" element={<DistributionForm />} />
            <Route path="/relief-distributions/:id" element={<DistributionDetail />} />
            <Route path="/ground-reports" element={<GroundReportList />} />
            <Route path="/ground-reports/:id" element={<GroundReportDetail />} />
            <Route path="/post-event-reports" element={<PostEventList />} />
            <Route path="/post-event-reports/:id" element={<PostEventDetail />} />
            <Route path="*" element={<p className="empty">Page not found.</p>} />
          </Routes>
        </main>
      </div>
      <div className="toasts">
        {toasts.map((t) => <div key={t.id} className={`toast ${t.kind}`}>{t.text}</div>)}
      </div>
    </ToastProvider>
  );
}
