import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { getClaims, getPayerAnalytics, getReasonAnalytics } from './api/client.js';

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const shortMoney = new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1,
});
const date = new Intl.DateTimeFormat('en-US', {
  month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC',
});
const PAGE_SIZE = 10;

function useAnalytics(fetchData) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    fetchData(controller.signal)
      .then(setData)
      .catch((requestError) => {
        if (requestError.name !== 'AbortError') setError(requestError.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [fetchData]);

  return { data, loading, error };
}

function PayerSummary({ payerAnalytics }) {
  const { data: payers, loading, error } = payerAnalytics;
  const totalAmount = payers?.reduce((total, payer) => total + Number(payer.totalDeniedAmount), 0) ?? 0;
  const totalClaims = payers?.reduce((total, payer) => total + payer.deniedClaimCount, 0) ?? 0;

  return (
    <section className="summary-section" aria-labelledby="overview-heading">
      <div className="section-heading">
        <div>
          <div className="eyebrow">OVERVIEW</div>
          <h2 id="overview-heading">Denial exposure</h2>
        </div>
        <span className="metric-note">Current denied claims</span>
      </div>
      <div className="summary-grid">
        <article className="metric-card">
          <span className="metric-label">Billed amount denied</span>
          {loading ? <span className="metric-value skeleton" aria-label="Loading amount" />
            : error ? <span className="metric-unavailable">Unavailable</span>
              : <strong className="metric-value">{money.format(totalAmount)}</strong>}
          <span className="metric-footnote">Sum of billed amounts for claims currently denied</span>
        </article>
        <article className="metric-card count-card">
          <span className="metric-label">Denied claims</span>
          {loading ? <span className="metric-value skeleton" aria-label="Loading count" />
            : error ? <span className="metric-unavailable">Unavailable</span>
              : <strong className="metric-value">{totalClaims.toLocaleString('en-US')}</strong>}
          <span className="metric-footnote">Across all payers</span>
        </article>
        <article className="chart-card">
          <div className="card-heading">
            <div><h3>Denied billed amount by payer</h3><p>Sorted from highest to lowest</p></div>
          </div>
          {loading ? <div className="message chart-message" role="status">Loading payer analytics…</div>
            : error ? <div className="message error chart-message" role="alert">Unable to load payer analytics: {error}</div>
              : payers.length === 0 ? <div className="message chart-message">No denied claims to chart.</div>
                : (
                  <div className="chart-wrap">
                    <ResponsiveContainer width="100%" height={Math.max(250, payers.length * 43)}>
                      <BarChart data={payers} layout="vertical" margin={{ top: 4, right: 20, bottom: 4, left: 8 }}>
                        <CartesianGrid stroke="#edf0f4" horizontal={false} />
                        <XAxis type="number" tickFormatter={(value) => shortMoney.format(value)} tickLine={false}
                          axisLine={false} tick={{ fill: '#8490a0', fontSize: 10 }} />
                        <YAxis type="category" dataKey="payer" width={145} tickLine={false} axisLine={false}
                          tick={{ fill: '#596679', fontSize: 11 }} />
                        <Tooltip formatter={(value) => [money.format(Number(value)), 'Denied billed amount']}
                          cursor={{ fill: '#f6f8fb' }} />
                        <Bar dataKey="totalDeniedAmount" fill="#5278cc" radius={[0, 4, 4, 0]} maxBarSize={24} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}
          <p className="chart-caption">Amounts represent the billed amounts associated with claims currently marked denied.</p>
        </article>
      </div>
    </section>
  );
}

function ReasonSummary({ reasonAnalytics }) {
  const { data: reasons, loading, error } = reasonAnalytics;

  return (
    <section className="panel" aria-labelledby="reasons-heading">
      <div className="panel-heading">
        <div><h2 id="reasons-heading">Denial reasons</h2><p>Denied billed amount grouped by reason</p></div>
        <span className="data-label">{!loading && !error ? `${reasons.length} reasons` : 'Summary'}</span>
      </div>
      {loading ? <div className="message" role="status">Loading denial reasons…</div>
        : error ? <div className="message error" role="alert">Unable to load denial reasons: {error}</div>
          : reasons.length === 0 ? <div className="message">No denial reasons found.</div>
            : (
              <div className="table-wrap">
                <table className="reason-table">
                  <thead><tr><th scope="col">Denial reason</th><th scope="col" className="amount">Denied billed amount</th></tr></thead>
                  <tbody>{reasons.map((item, index) => (
                    <tr key={item.reason} className={index === 0 ? 'largest-row' : ''}>
                      <td>{item.reason}{index === 0 && <span className="largest-label">Highest</span>}</td>
                      <td className="amount">{money.format(Number(item.totalDeniedAmount))}</td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            )}
    </section>
  );
}

function ClaimsSection({ payers, reasons }) {
  const [filters, setFilters] = useState({ payer: '', reason: '', startDate: '', endDate: '' });
  const [pageNumber, setPageNumber] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const invalidDateRange = Boolean(filters.startDate && filters.endDate && filters.startDate > filters.endDate);

  useEffect(() => {
    const controller = new AbortController();
    setResult(null);
    setError('');
    if (invalidDateRange) {
      setLoading(false);
      setError('Start date must be on or before end date.');
      return () => controller.abort();
    }

    setLoading(true);
    getClaims({ ...filters, page: pageNumber, size: PAGE_SIZE }, controller.signal)
      .then(setResult)
      .catch((requestError) => {
        if (requestError.name !== 'AbortError') setError(requestError.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [filters, pageNumber, invalidDateRange]);

  function changeFilter(event) {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
    setPageNumber(0);
  }

  const claims = result?.content ?? [];
  const totalPages = result?.totalPages ?? 0;
  const currentPage = result?.page ?? pageNumber;

  return (
    <section className="panel claims-panel" aria-labelledby="claims-heading">
      <div className="panel-heading">
        <div><h2 id="claims-heading">Denied claims</h2><p>Filter and review individual claim records</p></div>
        {!loading && !error && result && <span className="data-label">{result.totalElements.toLocaleString('en-US')} matching</span>}
      </div>
      <div className="filter-bar" aria-label="Claim filters">
        <label className="filter-field"><span>Payer</span>
          <select name="payer" value={filters.payer} onChange={changeFilter}>
            <option value="">All payers</option>
            {(payers ?? []).map((item) => <option key={item.payer} value={item.payer}>{item.payer}</option>)}
          </select>
        </label>
        <label className="filter-field"><span>Denial reason</span>
          <select name="reason" value={filters.reason} onChange={changeFilter}>
            <option value="">All reasons</option>
            {(reasons ?? []).map((item) => <option key={item.reason} value={item.reason}>{item.reason}</option>)}
          </select>
        </label>
        <label className="filter-field"><span>Denied from</span>
          <input type="date" name="startDate" value={filters.startDate} max={filters.endDate || undefined} onChange={changeFilter} />
        </label>
        <label className="filter-field"><span>Denied through</span>
          <input type="date" name="endDate" value={filters.endDate} min={filters.startDate || undefined} onChange={changeFilter} />
        </label>
        <button className="clear-button" type="button" onClick={() => {
          setFilters({ payer: '', reason: '', startDate: '', endDate: '' });
          setPageNumber(0);
        }}>Clear filters</button>
      </div>

      {loading ? <div className="message" role="status">Loading denied claims…</div>
        : error ? <div className="message error" role="alert">{error}</div>
          : claims.length === 0 ? <div className="message">No denied claims match these filters.</div>
            : (
              <div className="table-wrap">
                <table>
                  <thead><tr>
                    <th scope="col">Claim ID</th><th scope="col">Payer</th><th scope="col">Provider</th>
                    <th scope="col">CPT code</th><th scope="col" className="amount">Billed amount</th>
                    <th scope="col">Denial reason</th><th scope="col">Denied at</th>
                  </tr></thead>
                  <tbody>{claims.map((claim) => (
                    <tr key={claim.id}>
                      <td className="claim-id">CLM-{String(claim.id).padStart(5, '0')}</td>
                      <td>{claim.payer}</td><td>{claim.provider}</td><td>{claim.cptCode}</td>
                      <td className="amount">{money.format(Number(claim.billedAmount))}</td>
                      <td>{claim.denialReason}</td>
                      <td>{claim.deniedAt ? date.format(new Date(`${claim.deniedAt}T00:00:00Z`)) : '—'}</td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            )}
      {!loading && !error && result && result.totalElements > 0 && (
        <div className="pagination">
          <span>Showing {currentPage * result.size + 1}–{Math.min((currentPage + 1) * result.size, result.totalElements)} of {result.totalElements}</span>
          <div className="page-controls">
            <button type="button" onClick={() => setPageNumber((page) => Math.max(0, page - 1))} disabled={currentPage <= 0}>Previous</button>
            <span>Page {currentPage + 1} of {Math.max(totalPages, 1)}</span>
            <button type="button" onClick={() => setPageNumber((page) => page + 1)} disabled={currentPage + 1 >= totalPages}>Next</button>
          </div>
        </div>
      )}
    </section>
  );
}

function App() {
  const payerAnalytics = useAnalytics(getPayerAnalytics);
  const reasonAnalytics = useAnalytics(getReasonAnalytics);

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="AR Denial Analytics home">
          <span className="brand-mark">AR</span><span>Revenue operations</span>
        </a>
        <nav aria-label="Main navigation">
          <a className="nav-link active" href="#overview">Overview</a>
          <a className="nav-link" href="#claims">Claims</a>
        </nav>
        <div className="profile" aria-label="Assessment workspace">AW</div>
      </header>
      <main id="top" className="content">
        <div className="eyebrow">REVENUE CYCLE</div>
        <div className="page-heading">
          <div><h1>AR Denial Analytics</h1><p className="subtitle">Understand denial exposure and review claim activity.</p></div>
        </div>
        <div id="overview"><PayerSummary payerAnalytics={payerAnalytics} /></div>
        <div className="lower-grid">
          <ReasonSummary reasonAnalytics={reasonAnalytics} />
          <aside className="method-note"><span className="method-icon">i</span><p><strong>How to read these amounts</strong><br />Denied amount is the sum of billed amounts for claims whose current status is denied. The source data does not include a separate denied amount.</p></aside>
        </div>
        <div id="claims" className="claims-anchor">
          <ClaimsSection payers={payerAnalytics.data} reasons={reasonAnalytics.data} />
        </div>
        <footer>AR Denial Analytics <span>·</span> Assessment dashboard</footer>
      </main>
    </div>
  );
}

export default App;
