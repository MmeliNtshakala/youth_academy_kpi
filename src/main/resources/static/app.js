// ── State ─────────────────────────────────────────────────────
let currentFlagAction = null; // { cadetId, action }
let flagChart = null;
let projectChart = null;

// ── View switching ────────────────────────────────────────────
function showView(id, el) {
  document.querySelectorAll('.view')
    .forEach(v => v.classList.remove('active'));
  document.getElementById('view-' + id).classList.add('active');

  document.querySelectorAll('.nav-item')
    .forEach(n => n.classList.remove('active'));
  el.classList.add('active');

  const titles = {
    dashboard: 'Dashboard',
    cadets:    'Cadet registry',
    flags:     'Flags & actions',
    reports:   'Reports'
  };
  document.getElementById('page-title').textContent = titles[id];

  if (id === 'dashboard') loadDashboard();
  if (id === 'cadets')    loadCadets();
  if (id === 'flags')     loadFlagView();
  if (id === 'reports')   loadReports();
}

// ── API helpers ───────────────────────────────────────────────
async function get(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error('GET failed: ' + url);
  return res.json();
}

async function post(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error('POST failed: ' + url);
  return res.json();
}

async function put(url, body) {
  const res = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error('PUT failed: ' + url);
  return res.json();
}

// ── Dashboard ─────────────────────────────────────────────────
async function loadDashboard() {
  try {
    const data = await get('/api/dashboard');

    document.getElementById('m-total').textContent =
      data.totalCadets;
    document.getElementById('m-active').textContent =
      data.activeCount;
    document.getElementById('m-flagged').textContent =
      (data.yellowCount + data.orangeCount + data.redCount);
    document.getElementById('m-attendance').textContent =
      data.averageAttendance + '%';

    renderFlagChart(data);
    renderProjectChart(data.attendanceByProject);
    renderRecentActivity(data.recentActivity);

  } catch (e) {
    console.error('Dashboard load failed:', e);
  }
}

function renderFlagChart(data) {
  const ctx = document.getElementById('flagChart').getContext('2d');
  if (flagChart) flagChart.destroy();
  flagChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['No flag', 'Yellow', 'Orange', 'Red'],
      datasets: [{
        data: [
          data.activeCount,
          data.yellowCount,
          data.orangeCount,
          data.redCount
        ],
        backgroundColor: ['#639922','#EF9F27','#D85A30','#E24B4A'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: 'right', labels: { font: { size: 12 } } }
      }
    }
  });
}

function renderProjectChart(byProject) {
  const ctx = document.getElementById('projectChart').getContext('2d');
  if (projectChart) projectChart.destroy();
  const labels = Object.keys(byProject);
  const values = Object.values(byProject);
  projectChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Avg attendance %',
        data: values,
        backgroundColor: ['#378ADD','#1D9E75','#7F77DD','#EF9F27'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      scales: {
        y: { min: 0, max: 100,
          ticks: { callback: v => v + '%' } }
      },
      plugins: { legend: { display: false } }
    }
  });
}

function renderRecentActivity(events) {
  const el = document.getElementById('recent-activity');
  if (!events || events.length === 0) {
    el.innerHTML = '<p style="color:#aaa;font-size:12px">No recent activity.</p>';
    return;
  }
  const dotClass = {
    YELLOW: 'yellow', ORANGE: 'orange', RED: 'red',
    RESTORED: 'restored', NOTICE_SENT: 'notice', PLAN_AGREED: 'plan'
  };
  el.innerHTML = events.map(e => `
    <div class="tl-item">
      <div class="tl-dot ${dotClass[e.eventType] || ''}"></div>
      <div>
        <div class="tl-text">
          <strong>${e.cadetName}</strong> — ${e.description}
        </div>
        <div class="tl-date">${e.eventDate} · ${e.triggeredBy}</div>
      </div>
    </div>
  `).join('');
}

// ── Cadets ────────────────────────────────────────────────────
async function loadCadets() {
  const search  = document.getElementById('search').value.trim();
  const flag    = document.getElementById('filter-flag').value;
  const project = document.getElementById('filter-project').value;

  let url = '/api/cadets?';
  if (search)  url += 'search=' + encodeURIComponent(search) + '&';
  if (flag)    url += 'flag=' + encodeURIComponent(flag) + '&';
  if (project) url += 'project=' + encodeURIComponent(project) + '&';

  try {
    const cadets = await get(url);
    renderCadetTable(cadets);
  } catch (e) {
    console.error('Cadets load failed:', e);
  }
}

function renderCadetTable(cadets) {
  const tbody = document.getElementById('cadet-table');
  const count = document.getElementById('table-count');

  tbody.innerHTML = cadets.slice(0, 50).map(c => `
    <tr>
      <td>
        <div class="cadet-cell">
          <div class="avatar">${initials(c.fullName)}</div>
          <div>
            <div class="cadet-name">${c.fullName}</div>
            <div class="cadet-id">${c.cadetCode}</div>
          </div>
        </div>
      </td>
      <td>${c.project}</td>
      <td>${flagBadge(c.flagStatus)}</td>
      <td>${progressBar(c.attendancePercent)}</td>
      <td style="color:#aaa;font-size:12px">${c.lastContactDate || '—'}</td>
      <td style="font-size:12px">${c.projectManager}</td>
      <td>
        <div class="action-btns">
          ${actionButtons(c)}
        </div>
      </td>
    </tr>
  `).join('');

  count.textContent = 'Showing ' + Math.min(cadets.length, 50) +
    ' of ' + cadets.length + ' cadets';
}

function actionButtons(c) {
  const btns = [];
  if (c.flagStatus === 'NONE' || c.flagStatus === 'YELLOW') {
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id}, 'yellow')">
      Yellow
    </button>`);
  }
  if (c.flagStatus === 'YELLOW') {
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id}, 'orange')">
      Orange
    </button>`);
  }
  if (c.flagStatus === 'ORANGE') {
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id}, 'red')">
      Red
    </button>`);
  }
  if (c.flagStatus === 'RED') {
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id}, 'restore')">
      Restore
    </button>`);
  }
  return btns.join('');
}

// ── Flags view ────────────────────────────────────────────────
async function loadFlagView() {
  try {
    const data = await get('/api/dashboard');
    document.getElementById('f-yellow').textContent = data.yellowCount;
    document.getElementById('f-orange').textContent = data.orangeCount;
    document.getElementById('f-red').textContent    = data.redCount;

    const flagged = await get('/api/cadets?flag=YELLOW');
    const orange  = await get('/api/cadets?flag=ORANGE');
    const red     = await get('/api/cadets?flag=RED');
    renderFlagTable([...flagged, ...orange, ...red]);
  } catch (e) {
    console.error('Flag view load failed:', e);
  }
}

function renderFlagTable(cadets) {
  const tbody = document.getElementById('flag-table');
  tbody.innerHTML = cadets.map(c => `
    <tr>
      <td>
        <div class="cadet-cell">
          <div class="avatar">${initials(c.fullName)}</div>
          <div>
            <div class="cadet-name">${c.fullName}</div>
            <div class="cadet-id">${c.cadetCode}</div>
          </div>
        </div>
      </td>
      <td>${flagBadge(c.flagStatus)}</td>
      <td style="font-size:12px">${c.daysSinceFlag}d</td>
      <td style="font-size:12px">${c.project}</td>
      <td style="font-size:12px">${c.projectManager}</td>
      <td>
        <div class="action-btns">
          ${actionButtons(c)}
        </div>
      </td>
    </tr>
  `).join('');
}

// ── Reports ───────────────────────────────────────────────────
async function loadReports() {
  try {
    const data = await get('/api/dashboard');
    const flagged = data.yellowCount + data.orangeCount + data.redCount;
    const pct = v => Math.round(v / data.totalCadets * 100) + '%';
    const stats = [
      ['Total cadets enrolled',      data.totalCadets],
      ['In good standing (no flag)', data.activeCount  + ' (' + pct(data.activeCount) + ')'],
      ['Yellow flag',                data.yellowCount  + ' (' + pct(data.yellowCount) + ')'],
      ['Orange flag',                data.orangeCount  + ' (' + pct(data.orangeCount) + ')'],
      ['Red flag',                   data.redCount     + ' (' + pct(data.redCount) + ')'],
      ['Total under flag',           flagged           + ' (' + pct(flagged) + ')'],
      ['Under correction',           data.underCorrection],
      ['Average attendance',         data.averageAttendance + '%'],
      ['Programme duration',         '12 months'],
    ];
    document.getElementById('kpi-summary').innerHTML =
      stats.map(([k, v]) => `
        <div class="stat-row">
          <span class="stat-key">${k}</span>
          <span class="stat-val">${v}</span>
        </div>
      `).join('');
  } catch (e) {
    console.error('Reports load failed:', e);
  }
}

// ── Add cadet modal ───────────────────────────────────────────
function openModal()  {
  document.getElementById('modal').classList.add('open');
}
function closeModal() {
  document.getElementById('modal').classList.remove('open');
}

async function submitNewCadet() {
  const body = {
    cadetCode:        document.getElementById('new-code').value.trim(),
    fullName:         document.getElementById('new-name').value.trim(),
    project:          document.getElementById('new-project').value,
    attendancePercent: parseInt(document.getElementById('new-att').value) || 80,
    projectManager:   document.getElementById('new-pm').value.trim(),
    flagStatus:       'NONE',
    lastContactDate:  new Date().toISOString().split('T')[0]
  };
  if (!body.fullName || !body.cadetCode) {
    alert('Please enter a full name and cadet ID.');
    return;
  }
  try {
    await post('/api/cadets', body);
    closeModal();
    loadCadets();
    loadDashboard();
  } catch (e) {
    alert('Failed to add cadet. Check the console for details.');
  }
}

// ── Flag action modal ─────────────────────────────────────────
function openFlagModal(cadetId, action) {
  currentFlagAction = { cadetId, action };

  const titles = {
    yellow:  'Issue Yellow Flag',
    orange:  'Escalate to Orange Flag',
    red:     'Escalate to Red Flag',
    restore: 'Restore cadet status'
  };

  document.getElementById('flag-modal-title').textContent =
    titles[action];

  const reasonRow = document.getElementById('flag-reason-row');
  const notesRow  = document.getElementById('flag-notes-row');

  if (action === 'restore') {
    reasonRow.style.display = 'none';
    notesRow.style.display  = 'block';
  } else if (action === 'orange') {
    reasonRow.style.display = 'none';
    notesRow.style.display  = 'none';
  } else {
    reasonRow.style.display = 'block';
    notesRow.style.display  = 'none';
  }

  document.getElementById('flag-modal').classList.add('open');
}

function closeFlagModal() {
  document.getElementById('flag-modal').classList.remove('open');
  currentFlagAction = null;
}

async function submitFlagAction() {
  if (!currentFlagAction) return;

  const { cadetId, action } = currentFlagAction;
  const triggeredBy = document.getElementById('flag-triggered-by')
    .value.trim() || 'Admin';
  const reason = document.getElementById('flag-reason').value.trim()
    || 'No reason provided.';
  const notes  = document.getElementById('flag-notes').value.trim()
    || '';

  try {
    if (action === 'yellow') {
      await post(`/api/cadets/${cadetId}/flag/yellow`,
        { triggeredBy, reason });
    } else if (action === 'orange') {
      await post(`/api/cadets/${cadetId}/flag/orange`,
        { triggeredBy });
    } else if (action === 'red') {
      await post(`/api/cadets/${cadetId}/flag/red`,
        { triggeredBy, reason });
    } else if (action === 'restore') {
      await post(`/api/cadets/${cadetId}/restore`,
        { triggeredBy, notes });
    }

    closeFlagModal();
    loadCadets();
    loadFlagView();
    loadDashboard();

  } catch (e) {
    alert('Action failed. Check the console for details.');
  }
}

// ── Helpers ───────────────────────────────────────────────────
function initials(name) {
  return (name || '').split(' ')
    .map(w => w[0]).join('').slice(0, 2).toUpperCase();
}

function flagBadge(status) {
  const map = {
    NONE:   ['flag-none',   'No flag'],
    YELLOW: ['flag-yellow', 'Yellow'],
    ORANGE: ['flag-orange', 'Orange'],
    RED:    ['flag-red',    'Red']
  };
  const [cls, label] = map[status] || ['flag-none', status];
  return `<span class="flag ${cls}">${label}</span>`;
}

function progressBar(v) {
  const cls = v < 50 ? 'low' : v < 70 ? 'mid' : '';
  return `
    <div class="progress-wrap">
      <div class="progress-bar">
        <div class="progress-fill ${cls}" style="width:${v}%"></div>
      </div>
      <span class="progress-pct">${v}%</span>
    </div>`;
}

// ── Init ──────────────────────────────────────────────────────
loadDashboard();