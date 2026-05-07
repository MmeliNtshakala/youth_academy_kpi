// ── Current user ──────────────────────────────────────────────
let currentUser = null;
let currentFlagAction = null;
let flagChart = null;
let projectChart = null;
let reportRegionChart = null;
let currentMeetingId = null;
let registerEntries = {};

// ── Boot — load current user first ───────────────────────────
async function boot() {
  try {
    currentUser = await get('/api/me');
    applyUserContext();
    loadDashboard();
  } catch (e) {
    // Not logged in — redirect to login
    window.location.href = '/login';
  }
}

// ── Apply user context to the UI ──────────────────────────────
function applyUserContext() {
  if (!currentUser) return;

  // Update sidebar name and role
  const nameEl = document.getElementById('user-name');
  const roleEl = document.getElementById('user-role');
  if (nameEl) nameEl.textContent = currentUser.fullName;
  if (roleEl) roleEl.textContent =
    currentUser.isAdmin
      ? 'Admin · All regions'
      : 'Liaison · ' + currentUser.region;

  // Show Users tab only for Admin
  const usersTab = document.getElementById('nav-users');
  if (usersTab) {
    usersTab.style.display = currentUser.isAdmin ? 'block' : 'none';
  }

  // Show/hide admin-only action buttons
  document.querySelectorAll('.admin-only').forEach(el => {
    el.style.display = currentUser.isAdmin ? '' : 'none';
  });
}

// ── Region filter — Liaison sees only their region ────────────
function myRegion() {
  return currentUser && !currentUser.isAdmin
    ? currentUser.region
    : null;
}

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
    register:  'Register',
    reports:   'Reports',
    users:     'User management'
  };
  document.getElementById('page-title').textContent = titles[id];

  const btn = document.getElementById('top-action-btn');
  if (btn) btn.textContent =
    id === 'register' ? '+ New meeting' :
    id === 'users'    ? '+ Add user'    : '+ Add cadet';

  if (id === 'dashboard') loadDashboard();
  if (id === 'cadets')    loadCadets();
  if (id === 'flags')     loadFlagView();
  if (id === 'register')  loadRegisterLanding();
  if (id === 'reports')   loadReports();
  if (id === 'users')     loadUsers();
}

function handleTopAction() {
  const view = document.querySelector('.view.active').id;
  if (view === 'view-register') {
    document.getElementById('reg-date').value =
      new Date().toISOString().split('T')[0];
    window.scrollTo(0, 0);
  } else if (view === 'view-users') {
    openUserModal();
  } else {
    openModal();
  }
}

// ── API helpers ───────────────────────────────────────────────
async function get(url) {
  const res = await fetch(url);
  if (res.status === 401) { window.location.href = '/login'; return; }
  if (!res.ok) throw new Error('GET failed: ' + url);
  return res.json();
}

async function post(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (res.status === 401) { window.location.href = '/login'; return; }
  if (!res.ok) throw new Error('POST failed: ' + url);
  return res.json();
}

async function put(url, body) {
  const res = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  });
  if (res.status === 401) { window.location.href = '/login'; return; }
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
  } catch (e) { console.error('Dashboard load failed:', e); }
}

function renderFlagChart(data) {
  const ctx = document.getElementById('flagChart').getContext('2d');
  if (flagChart) flagChart.destroy();
  flagChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['No flag','Yellow','Orange','Red'],
      datasets: [{
        data: [data.activeCount, data.yellowCount,
               data.orangeCount, data.redCount],
        backgroundColor: ['#639922','#EF9F27','#D85A30','#E24B4A'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: 'right',
          labels: { font: { size: 12 } } }
      }
    }
  });
}

function renderProjectChart(byProject) {
  const ctx =
    document.getElementById('projectChart').getContext('2d');
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
      scales: { y: { min: 0, max: 100,
        ticks: { callback: v => v + '%' } } },
      plugins: { legend: { display: false } }
    }
  });
}

function renderRecentActivity(events) {
  const el = document.getElementById('recent-activity');
  if (!events || events.length === 0) {
    el.innerHTML =
      '<p style="color:#aaa;font-size:12px">No recent activity.</p>';
    return;
  }
  const dotClass = {
    YELLOW: 'yellow', ORANGE: 'orange', RED: 'red',
    RESTORED: 'restored', NOTICE_SENT: 'notice', PLAN_AGREED: 'plan'
  };
  el.innerHTML = `<div class="timeline">` +
    events.map(e => `
      <div class="tl-item">
        <div class="tl-dot ${dotClass[e.eventType] || ''}"></div>
        <div>
          <div class="tl-text">
            <strong>${e.cadetName}</strong> — ${e.description}
          </div>
          <div class="tl-date">
            ${e.eventDate} · ${e.triggeredBy}
          </div>
        </div>
      </div>`).join('') + `</div>`;
}

// ── Cadets ────────────────────────────────────────────────────
async function loadCadets() {
  const search  = document.getElementById('search').value.trim();
  const flag    = document.getElementById('filter-flag').value;
  const region  = myRegion() ||
    document.getElementById('filter-project').value;

  let url = '/api/cadets?';
  if (search) url += 'search=' + encodeURIComponent(search) + '&';
  if (flag)   url += 'flag='   + encodeURIComponent(flag) + '&';
  if (region) url += 'project='+ encodeURIComponent(region) + '&';

  try {
    const cadets = await get(url);
    renderCadetTable(cadets);
  } catch (e) { console.error('Cadets load failed:', e); }
}

function renderCadetTable(cadets) {
  const regions = myRegion()
    ? [myRegion()]
    : ['Gauteng','eMazweni','eMangalisweni','eZenzweni', 'International', 'Zimbabwe', 'Mozambique'];

  const grouped = {};
  regions.forEach(r => grouped[r] = []);
  cadets.forEach(c => {
    const r = c.project;
    if (grouped[r]) grouped[r].push(c);
    else grouped[r] = [c];
  });

  const container = document.getElementById('cadet-regions');
  const countEl   = document.getElementById('table-count');
  let html = '';

  regions.forEach(region => {
    const list = grouped[region] || [];
    if (list.length === 0) return;

    const regionKey = region.replace(/\s/g,'-');
    const avgAtt = Math.round(
      list.reduce((s,c) => s + c.attendancePercent, 0) / list.length
    );

    html += `
      <div class="region-folder">
        <div class="region-folder-header"
          onclick="toggleFolder('${regionKey}')">
          <div style="display:flex;align-items:center;gap:10px">
            <span class="folder-arrow"
              id="arrow-${regionKey}">▶</span>
            <span class="region-folder-name">${region}</span>
            <span class="region-folder-count">
              ${list.length} cadets
            </span>
          </div>
          <div style="display:flex;align-items:center;gap:12px">
            ${list.filter(c=>c.flagStatus==='YELLOW').length > 0
              ? `<span class="flag flag-yellow">
                  ${list.filter(c=>c.flagStatus==='YELLOW').length}
                  yellow</span>` : ''}
            ${list.filter(c=>c.flagStatus==='ORANGE').length > 0
              ? `<span class="flag flag-orange">
                  ${list.filter(c=>c.flagStatus==='ORANGE').length}
                  orange</span>` : ''}
            ${list.filter(c=>c.flagStatus==='RED').length > 0
              ? `<span class="flag flag-red">
                  ${list.filter(c=>c.flagStatus==='RED').length}
                  red</span>` : ''}
            <span style="font-size:12px;color:#888">
              avg ${avgAtt}% attendance
            </span>
          </div>
        </div>
        <div class="region-folder-body"
          id="body-${regionKey}" style="display:none">
          <table>
            <thead>
              <tr>
                <th>Cadet</th>
                <th>Flag</th>
                <th>Attendance</th>
                <th>Last contact</th>
                <th>Liaison</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              ${list.map(c => `
                <tr>
                  <td>
                    <div class="cadet-cell">
                      <div class="avatar">
                        ${initials(c.fullName)}
                      </div>
                      <div>
                        <div class="cadet-name">${c.fullName}</div>
                        <div class="cadet-id">${c.cadetCode}</div>
                      </div>
                    </div>
                  </td>
                  <td>${flagBadge(c.flagStatus)}</td>
                  <td>${progressBar(c.attendancePercent)}</td>
                  <td style="color:#aaa;font-size:12px">
                    ${c.lastContactDate || '—'}
                  </td>
                  <td style="font-size:12px">
                    ${c.projectManager}
                  </td>
                  <td>
                    <div class="action-btns">
                      ${actionButtons(c)}
                    </div>
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`;
  });

  container.innerHTML = html ||
    '<p style="color:#aaa;font-size:13px;padding:12px 0">' +
    'No cadets found. Add cadets to get started.</p>';
  countEl.textContent = cadets.length + ' cadet(s)';
}

function toggleFolder(regionKey) {
  const body  = document.getElementById('body-'  + regionKey);
  const arrow = document.getElementById('arrow-' + regionKey);
  if (!body) return;
  const isOpen = body.style.display !== 'none';
  body.style.display = isOpen ? 'none' : 'block';
  arrow.textContent  = isOpen ? '▶' : '▼';
}

function actionButtons(c) {
  const btns = [];
  if (c.flagStatus === 'NONE' || c.flagStatus === 'YELLOW')
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id},'yellow')">
      Yellow</button>`);
  if (c.flagStatus === 'YELLOW')
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id},'orange')">
      Orange</button>`);
  if (c.flagStatus === 'ORANGE')
    btns.push(`<button class="icon-btn"
      onclick="openFlagModal(${c.id},'red')">
      Red</button>`);
  if (c.flagStatus === 'RED' && currentUser && currentUser.isAdmin)
    btns.push(`<button class="icon-btn admin-only"
      onclick="openFlagModal(${c.id},'restore')">
      Restore</button>`);
  return btns.join('');
}

// ── Flags ─────────────────────────────────────────────────────
async function loadFlagView() {
  try {
    const data = await get('/api/dashboard');
    const fy = document.getElementById('f-yellow');
    const fo = document.getElementById('f-orange');
    const fr = document.getElementById('f-red');
    if (fy) fy.textContent = data.yellowCount  || 0;
    if (fo) fo.textContent = data.orangeCount  || 0;
    if (fr) fr.textContent = data.redCount     || 0;
  } catch (e) { console.error('Dashboard metrics failed:', e); }

  try {
    const region = myRegion();
    const yUrl = '/api/cadets?flag=YELLOW' +
      (region ? '&project=' + encodeURIComponent(region) : '');
    const oUrl = '/api/cadets?flag=ORANGE' +
      (region ? '&project=' + encodeURIComponent(region) : '');
    const rUrl = '/api/cadets?flag=RED' +
      (region ? '&project=' + encodeURIComponent(region) : '');
    const y = await get(yUrl);
    const o = await get(oUrl);
    const r = await get(rUrl);
    renderFlagTable([...y, ...o, ...r]);
  } catch (e) { console.error('Flagged cadets failed:', e); }
}

function renderFlagTable(cadets) {
  const regions = myRegion()
    ? [myRegion()]
    : ['Gauteng','eMazweni','eMangalisweni','eZenzweni', 'Zimbabwe', 'Internationl', 'Mozambique'];

  const grouped = {};
  regions.forEach(r => grouped[r] = []);
  cadets.forEach(c => {
    const r = c.project;
    if (grouped[r]) grouped[r].push(c);
    else grouped[r] = [c];
  });

  const container = document.getElementById('flag-table-container');
  if (!container) return;

  if (cadets.length === 0) {
    container.innerHTML =
      '<p style="color:#aaa;font-size:13px;padding:8px 0">' +
      'No flagged cadets.</p>';
    return;
  }

  let html = '';
  regions.forEach(region => {
    const list = grouped[region] || [];
    if (list.length === 0) return;
    const regionKey = region.replace(/\s/g,'-');
    html += `
      <div class="region-folder">
        <div class="region-folder-header"
          onclick="toggleFlagFolder('${regionKey}')">
          <div style="display:flex;align-items:center;gap:10px">
            <span class="folder-arrow"
              id="flag-arrow-${regionKey}">▶</span>
            <span class="region-folder-name">${region}</span>
            <span class="region-folder-count">
              ${list.length} flagged
            </span>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            ${list.filter(c=>c.flagStatus==='YELLOW').length > 0
              ? `<span class="flag flag-yellow">
                ${list.filter(c=>c.flagStatus==='YELLOW').length}
                yellow</span>` : ''}
            ${list.filter(c=>c.flagStatus==='ORANGE').length > 0
              ? `<span class="flag flag-orange">
                ${list.filter(c=>c.flagStatus==='ORANGE').length}
                orange</span>` : ''}
            ${list.filter(c=>c.flagStatus==='RED').length > 0
              ? `<span class="flag flag-red">
                ${list.filter(c=>c.flagStatus==='RED').length}
                red</span>` : ''}
          </div>
        </div>
        <div class="region-folder-body"
          id="flag-body-${regionKey}" style="display:none">
          <table>
            <thead>
              <tr>
                <th>Cadet</th>
                <th>Flag</th>
                <th>Days since flag</th>
                <th>Liaison</th>
                <th>Next action</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              ${list.map(c => `
                <tr>
                  <td>
                    <div class="cadet-cell">
                      <div class="avatar">
                        ${initials(c.fullName)}
                      </div>
                      <div>
                        <div class="cadet-name">${c.fullName}</div>
                        <div class="cadet-id">${c.cadetCode}</div>
                      </div>
                    </div>
                  </td>
                  <td>${flagBadge(c.flagStatus)}</td>
                  <td style="font-size:12px">${c.daysSinceFlag}d</td>
                  <td style="font-size:12px">${c.projectManager}</td>
                  <td style="font-size:11px;color:#888">
                    ${c.flagStatus === 'YELLOW'
                      ? 'Escalate to Orange if no re-engagement'
                      : c.flagStatus === 'ORANGE'
                      ? 'Refer to Liaison'
                      : 'Disciplinary Subcommittee review'}
                  </td>
                  <td>
                    <div class="action-btns">
                      ${actionButtons(c)}
                    </div>
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`;
  });

  container.innerHTML = html;
}

function toggleFlagFolder(regionKey) {
  const body  = document.getElementById('flag-body-' + regionKey);
  const arrow = document.getElementById('flag-arrow-' + regionKey);
  if (!body) return;
  const isOpen = body.style.display !== 'none';
  body.style.display = isOpen ? 'none' : 'block';
  arrow.textContent  = isOpen ? '▶' : '▼';
}

// ── Register ──────────────────────────────────────────────────
async function loadRegisterLanding() {
  backToLanding();
  document.getElementById('reg-date').value =
    new Date().toISOString().split('T')[0];

  // Pre-select region for Liaison
  if (myRegion()) {
    const regRegion = document.getElementById('reg-region');
    if (regRegion) regRegion.value = myRegion();
  }

  loadMeetings();
  loadIncomplete();
}

async function loadMeetings() {
  const region = myRegion() ||
    document.getElementById('meeting-region-filter').value;
  let url = '/api/meetings';
  if (region) url += '?region=' + encodeURIComponent(region);
  try {
    const meetings = await get(url);
    renderMeetingsTable(meetings);
  } catch (e) { console.error('Meetings load failed:', e); }
}

function renderMeetingsTable(meetings) {
  const tbody = document.getElementById('meetings-table');
  if (meetings.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6"
      style="color:#aaa;font-size:12px;padding:12px 10px">
      No meetings yet. Create one above.</td></tr>`;
    return;
  }
  tbody.innerHTML = meetings.map(m => `
    <tr>
      <td style="font-weight:500;font-size:13px">${m.title}</td>
      <td style="font-size:12px;color:#888">${m.meetingType}</td>
      <td style="font-size:12px">${m.region}</td>
      <td style="font-size:12px;color:#888">${m.meetingDate}</td>
      <td>
        <span class="meeting-status
          ${m.registerComplete
            ? 'meeting-done' : 'meeting-pending'}">
          ${m.registerComplete ? 'Complete' : 'Pending'}
        </span>
      </td>
      <td>
        <button class="icon-btn"
          onclick="openRegisterSheet(${m.id},
            '${m.title.replace(/'/g,"\\'")}',
            '${m.meetingDate}','${m.region}')">
          ${m.registerComplete ? 'View' : 'Take register'}
        </button>
      </td>
    </tr>`).join('');
}

async function loadIncomplete() {
  try {
    const list = await get('/api/meetings/incomplete');
    const el = document.getElementById('incomplete-list');
    if (list.length === 0) {
      el.innerHTML =
        '<p style="color:#aaa;font-size:12px">' +
        'All registers are up to date.</p>';
      return;
    }
    const filtered = myRegion()
      ? list.filter(m =>
          m.region === myRegion() || m.region === 'ALL')
      : list;
    el.innerHTML = filtered.map(m => `
      <div class="pending-card">
        <div>
          <div style="font-weight:500">${m.title}</div>
          <div style="color:#aaa;font-size:11px">
            ${m.region} · ${m.meetingDate}
          </div>
        </div>
        <button class="icon-btn"
          onclick="openRegisterSheet(${m.id},
            '${m.title.replace(/'/g,"\\'")}',
            '${m.meetingDate}','${m.region}')">
          Take register
        </button>
      </div>`).join('');
  } catch (e) { console.error('Incomplete load failed:', e); }
}

async function createMeeting() {
  const region = myRegion() ||
    document.getElementById('reg-region').value;
  const body = {
    meetingType: document.getElementById('reg-type').value,
    title:       document.getElementById('reg-title').value.trim(),
    region,
    meetingDate: document.getElementById('reg-date').value,
    createdBy:   currentUser
      ? currentUser.fullName : 'Admin',
    notes:       document.getElementById('reg-notes').value.trim()
  };
  if (!body.meetingDate) {
    alert('Please select a date.'); return;
  }
  try {
    const result = await post('/api/meetings', body);
    if (result.success) {
      document.getElementById('reg-title').value = '';
      document.getElementById('reg-notes').value = '';
      loadMeetings();
      loadIncomplete();
    } else {
      alert(result.message);
    }
  } catch (e) { alert('Failed to create meeting.'); }
}

async function openRegisterSheet(meetingId, title, date, region) {
  currentMeetingId = meetingId;
  registerEntries  = {};
  document.getElementById('register-landing').style.display = 'none';
  document.getElementById('register-sheet').style.display  = 'block';
  document.getElementById('sheet-title').textContent = title;
  document.getElementById('sheet-subtitle').textContent =
    region + ' · ' + date;
  document.getElementById('register-banner').style.display = 'none';
  document.getElementById('register-groups').innerHTML =
    '<p style="color:#aaa;font-size:13px">Loading cadets...</p>';

  if (currentUser) {
    const recBy = document.getElementById('register-recorded-by');
    if (recBy) recBy.value = currentUser.fullName;
  }

  try {
    const data =
      await get('/api/meetings/' + meetingId + '/register');
    renderRegisterGroups(data.register);
  } catch (e) {
    document.getElementById('register-groups').innerHTML =
      '<p style="color:#a32d2d;font-size:13px">' +
      'Failed to load register.</p>';
  }
}

function backToLanding() {
  document.getElementById('register-landing').style.display = 'block';
  document.getElementById('register-sheet').style.display  = 'none';
  currentMeetingId = null;
  registerEntries  = {};
}

function renderRegisterGroups(grouped) {
  const container = document.getElementById('register-groups');
  const regions = myRegion()
    ? [myRegion()]
    : ['Gauteng','eMazweni','eMangalisweni','eZenzweni', 'International', 'Zimbabwe', 'Mozambique'];
  let html = '';

  for (const region of regions) {
    const cadets = grouped[region];
    if (!cadets || cadets.length === 0) continue;
    html += `
      <div class="province-group">
        <div class="province-heading">
          ${region}
          <span class="province-count">${cadets.length} cadets</span>
        </div>
        <div class="register-header">
          <span></span><span>Cadet</span>
          <span>Status</span><span>Absence reason</span>
          <span></span>
        </div>`;

    cadets.forEach((c, i) => {
      registerEntries[c.cadetId] = {
        cadetId: c.cadetId, cadetCode: c.cadetCode,
        fullName: c.fullName, region,
        status: c.status || '', absenceReason: c.absenceReason || ''
      };
      const isAbsent = c.status === 'ABSENT';
      html += `
        <div class="register-row ${isAbsent ? 'absent-row' : ''}"
          id="row-${c.cadetId}">
          <span class="reg-num">${i + 1}</span>
          <div>
            <div class="reg-name">${c.fullName}</div>
            <div class="reg-code">${c.cadetCode}</div>
          </div>
          <div class="status-btn-group">
            <button class="status-btn
              ${c.status==='PRESENT' ? 'selected-present' : ''}"
              onclick="setStatus(${c.cadetId},'PRESENT')">
              Present
            </button>
            <button class="status-btn
              ${c.status==='ABSENT' ? 'selected-absent' : ''}"
              onclick="setStatus(${c.cadetId},'ABSENT')">
              Absent
            </button>
            <button class="status-btn
              ${c.status==='LATE' ? 'selected-late' : ''}"
              onclick="setStatus(${c.cadetId},'LATE')">
              Late
            </button>
          </div>
          <div>
            <input class="reason-input
              ${c.absenceReason ? 'filled' : ''}"
              id="reason-${c.cadetId}"
              placeholder="${isAbsent
                ? 'Required — enter reason'
                : 'Only needed if absent'}"
              value="${c.absenceReason || ''}"
              style="${isAbsent ? '' : 'display:none'}"
              oninput="setReason(${c.cadetId}, this.value)">
          </div>
          <div></div>
        </div>`;
    });
    html += `</div>`;
  }

  container.innerHTML = html ||
    '<p style="color:#aaa">No cadets found in this region.</p>';
}

function setStatus(cadetId, status) {
  if (!registerEntries[cadetId]) return;
  registerEntries[cadetId].status = status;
  const row         = document.getElementById('row-' + cadetId);
  const reasonInput = document.getElementById('reason-' + cadetId);
  const btns        = row.querySelectorAll('.status-btn');
  btns.forEach(b => b.classList.remove(
    'selected-present','selected-absent','selected-late'));
  if (status === 'PRESENT') btns[0].classList.add('selected-present');
  if (status === 'ABSENT')  btns[1].classList.add('selected-absent');
  if (status === 'LATE')    btns[2].classList.add('selected-late');
  if (status === 'ABSENT') {
    reasonInput.style.display = 'block';
    reasonInput.placeholder   = 'Required — enter reason';
    row.classList.add('absent-row');
  
  } else {
    reasonInput.style.display = 'none';
    reasonInput.value         = '';
    registerEntries[cadetId].absenceReason = '';
    row.classList.remove('absent-row');
  }
}

function setReason(cadetId, value) {
  if (!registerEntries[cadetId]) return;
  registerEntries[cadetId].absenceReason = value;
  const input = document.getElementById('reason-' + cadetId);
  input.classList.toggle('filled', value.trim().length > 0);
}

function filterSheetRows() {
  const q = document.getElementById('sheet-search')
    .value.toLowerCase();
  document.querySelectorAll('.register-row').forEach(row => {
    const name = row.querySelector('.reg-name');
    if (!name) return;
    row.style.display =
      name.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

async function submitRegister() {
  const recordedBy =
    document.getElementById('register-recorded-by').value.trim();
  if (!recordedBy) {
    showBanner('error',
      'Please enter your name before submitting.');
    return;
  }
  const entries = Object.values(registerEntries);
  const noStatus = entries.filter(e => !e.status);
  if (noStatus.length > 0) {
    showBanner('error',
      noStatus.length + ' cadet(s) have no status selected.');
    return;
  }
  const missingReason = entries.filter(
    e => e.status === 'ABSENT' &&
    (!e.absenceReason || !e.absenceReason.trim())
  );
  if (missingReason.length > 0) {
    showBanner('error',
      'Absence reason required for: ' +
      missingReason.map(e => e.fullName).join(', '));
    missingReason.forEach(e => {
      const input = document.getElementById('reason-' + e.cadetId);
      if (input) { input.style.borderColor = '#e24b4a'; input.focus(); }
    });
    return;
  }
  try {
    const result = await post(
      '/api/meetings/' + currentMeetingId + '/register',
      { recordedBy, entries }
    );
    if (result.success) {
      let msg = 'Register submitted. ' + result.saved + ' records saved.';
      let type = 'success';
      if (result.autoFlagged && result.autoFlagged.length > 0) {
        msg += ' Yellow Flag auto-issued for: ' +
          result.autoFlagged.join(', ');
        type = 'warning';
      }
      showBanner(type, msg);
      loadMeetings();
      loadIncomplete();
    } else {
      showBanner('error', result.message);
    }
  } catch (e) {
    showBanner('error', 'Submission failed. Please try again.');
  }
}

function showBanner(type, msg) {
  const el = document.getElementById('register-banner');
  el.className = 'banner banner-' + type;
  el.textContent = msg;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// ── Reports ───────────────────────────────────────────────────
async function loadReports() {
  try {
    const data = await get('/api/dashboard');
    const flagged =
      data.yellowCount + data.orangeCount + data.redCount;
    const pct = v =>
      Math.round(v / (data.totalCadets || 1) * 100) + '%';
    const stats = [
      ['Total cadets',           data.totalCadets],
      ['Good standing',          data.activeCount +
        ' (' + pct(data.activeCount) + ')'],
      ['Yellow flag',            data.yellowCount +
        ' (' + pct(data.yellowCount) + ')'],
      ['Orange flag',            data.orangeCount +
        ' (' + pct(data.orangeCount) + ')'],
      ['Red flag',               data.redCount +
        ' (' + pct(data.redCount) + ')'],
      ['Total under flag',       flagged +
        ' (' + pct(flagged) + ')'],
      ['Under correction',       data.underCorrection],
      ['Average attendance',     data.averageAttendance + '%'],

    ];
    document.getElementById('summary').innerHTML =
      stats.map(([k,v]) => `
        <div class="stat-row">
          <span class="stat-key">${k}</span>
          <span class="stat-val">${v}</span>
        </div>`).join('');

    try {
      const regionData =
        await get('/api/meetings/region-summary');
      const byRegion = regionData.byRegion || {};
      const ctx = document
        .getElementById('reportRegionChart').getContext('2d');
      if (reportRegionChart) reportRegionChart.destroy();
      reportRegionChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: Object.keys(byRegion),
          datasets: [{
            label: 'Attendance %',
            data: Object.values(byRegion),
            backgroundColor:
              ['#378ADD','#1D9E75','#7F77DD','#EF9F27'],
            borderWidth: 0
          }]
        },
        options: {
          responsive: true,
          scales: { y: { min:0, max:100,
            ticks: { callback: v => v + '%' } } },
          plugins: { legend: { display: false } }
        }
      });
    } catch (e) {
      document.getElementById('reportRegionChart')
        .parentElement.innerHTML =
        '<p style="color:#aaa;font-size:12px;padding:12px">' +
        'No attendance data yet.</p>';
    }
  } catch (e) { console.error('Reports load failed:', e); }
}

async function searchCadetReport() {
  const q = document.getElementById('report-search').value.trim();
  if (q.length < 2) {
    document.getElementById('report-cadet-results').innerHTML = '';
    return;
  }
  try {
    const cadets = await get(
      '/api/cadets?search=' + encodeURIComponent(q)
    );
    document.getElementById('report-cadet-results').innerHTML =
      cadets.slice(0,8).map(c => `
        <div class="pending-card" style="cursor:pointer"
          onclick="loadCadetReport(${c.id})">
          <div>
            <div style="font-weight:500">${c.fullName}</div>
            <div style="color:#aaa;font-size:11px">
              ${c.cadetCode} · ${c.project}
            </div>
          </div>
          <span style="font-size:12px;color:#888">
            ${c.attendancePercent}% attendance
          </span>
        </div>`).join('');
  } catch (e) { console.error('Search failed:', e); }
}

async function loadCadetReport(cadetId) {
  try {
    const data =
      await get('/api/meetings/cadet/' + cadetId + '/report');
    document.getElementById('report-cadet-detail').innerHTML = `
      <div style="margin-top:16px;padding-top:16px;
        border-top:0.5px solid #e0e0dc">
        <div style="display:flex;align-items:center;
          gap:12px;margin-bottom:14px">
          <div class="avatar"
            style="width:36px;height:36px;font-size:12px">
            ${initials(data.cadet.fullName)}
          </div>
          <div>
            <div style="font-weight:500">${data.cadet.fullName}</div>
            <div style="font-size:12px;color:#aaa">
              ${data.cadet.cadetCode} · ${data.cadet.project}
            </div>
          </div>
          <div style="margin-left:auto;display:flex;
            gap:20px;text-align:center">
            <div>
              <div style="font-size:20px;font-weight:500;
                color:#3b6d11">${data.attendancePct}%</div>
              <div style="font-size:11px;color:#aaa">attendance</div>
            </div>
            <div>
              <div style="font-size:20px;font-weight:500">
                ${data.attended}/${data.totalMeetings}</div>
              <div style="font-size:11px;color:#aaa">meetings</div>
            </div>
            <div>
              <div style="font-size:20px;font-weight:500;
                color:${data.absences > 0 ? '#a32d2d' : '#1a1a1a'}">
                ${data.absences}</div>
              <div style="font-size:11px;color:#aaa">absences</div>
            </div>
          </div>
        </div>
        ${data.history.length === 0
          ? '<p style="color:#aaa;font-size:12px">No records yet.</p>'
          : `<table><thead><tr>
              <th>Date</th><th>Status</th>
              <th>Reason</th><th>Recorded by</th>
             </tr></thead><tbody>
             ${data.history.map(r => `<tr>
               <td style="font-size:12px">${r.meetingDate}</td>
               <td>${attendanceBadge(r.status)}</td>
               <td style="font-size:12px;color:#888">
                 ${r.absenceReason || '—'}</td>
               <td style="font-size:12px;color:#aaa">
                 ${r.recordedBy}</td>
             </tr>`).join('')}
             </tbody></table>`}
      </div>`;
  } catch (e) { console.error('Cadet report failed:', e); }
}

function attendanceBadge(status) {
  const map = {
    PRESENT: ['flag-none',   'Present'],
    ABSENT:  ['flag-red',    'Absent'],
    LATE:    ['flag-yellow', 'Late']
  };
  const [cls, label] = map[status] || ['', status];
  return `<span class="flag ${cls}">${label}</span>`;
}

// ── User management (Admin only) ──────────────────────────────
async function loadUsers() {
  try {
    const users = await get('/api/users');
    renderUsersTable(users);
  } catch (e) { console.error('Users load failed:', e); }
}

function renderUsersTable(users) {
  const el = document.getElementById('users-table');
  if (!el) return;
  el.innerHTML = users.map(u => `
    <tr>
      <td>
        <div class="cadet-cell">
          <div class="avatar">${initials(u.fullName)}</div>
          <div>
            <div class="cadet-name">${u.fullName}</div>
            <div class="cadet-id">${u.username}</div>
          </div>
        </div>
      </td>
      <td style="font-size:12px">${u.role}</td>
      <td style="font-size:12px">${u.region}</td>
      <td>
        <span class="flag ${u.active ? 'flag-none' : 'flag-red'}">
          ${u.active ? 'Active' : 'Inactive'}
        </span>
      </td>
      <td>
        <div class="action-btns">
          ${u.role !== 'ADMIN' ? `
            <button class="icon-btn"
              onclick="toggleUserActive(${u.id}, ${u.active})">
              ${u.active ? 'Deactivate' : 'Activate'}
            </button>
            <button class="icon-btn"
              onclick="openResetPassword(${u.id}, '${u.fullName}')">
              Reset password
            </button>` : '<span style="font-size:11px;color:#aaa">Admin</span>'}
        </div>
      </td>
    </tr>`).join('');
}

async function toggleUserActive(id, isActive) {
  try {
    const url = '/api/users/' + id +
      (isActive ? '/deactivate' : '/activate');
    const result = await put(url);
    if (result.success) loadUsers();
    else alert(result.message);
  } catch (e) { alert('Action failed.'); }
}

// User modal
function openUserModal() {
  document.getElementById('user-modal').classList.add('open');
}
function closeUserModal() {
  document.getElementById('user-modal').classList.remove('open');
}

async function submitNewUser() {
  const body = {
    username: document.getElementById('u-username').value.trim(),
    password: document.getElementById('u-password').value.trim(),
    fullName: document.getElementById('u-fullname').value.trim(),
    role:     document.getElementById('u-role').value,
    region:   document.getElementById('u-region').value
  };
  if (!body.username || !body.password || !body.fullName) {
    alert('Please fill in all fields.'); return;
  }
  try {
    const result = await post('/api/users', body);
    if (result.success) { closeUserModal(); loadUsers(); }
    else alert(result.message);
  } catch (e) { alert('Failed to create user.'); }
}

// Reset password modal
function openResetPassword(id, name) {
  document.getElementById('rp-user-name').textContent = name;
  document.getElementById('rp-user-id').value = id;
  document.getElementById('rp-modal').classList.add('open');
}
function closeResetModal() {
  document.getElementById('rp-modal').classList.remove('open');
}

async function submitResetPassword() {
  const id  = document.getElementById('rp-user-id').value;
  const pwd = document.getElementById('rp-password').value.trim();
  if (!pwd || pwd.length < 6) {
    alert('Password must be at least 6 characters.'); return;
  }
  try {
    const result = await put(
      '/api/users/' + id + '/password', { newPassword: pwd }
    );
    if (result.success) { closeResetModal(); alert(result.message); }
    else alert(result.message);
  } catch (e) { alert('Failed to reset password.'); }
}

// ── Add cadet modal ───────────────────────────────────────────
function openModal() {
  // Pre-select region for Liaison
  if (myRegion()) {
    const sel = document.getElementById('new-project');
    if (sel) sel.value = myRegion();
  }
  document.getElementById('modal').classList.add('open');
}
function closeModal() {
  document.getElementById('modal').classList.remove('open');
}

async function submitNewCadet() {
  const body = {
    cadetCode:         document.getElementById('new-code')
      .value.trim(),
    fullName:          document.getElementById('new-name')
      .value.trim(),
    project:           document.getElementById('new-project').value,
    attendancePercent: parseInt(
      document.getElementById('new-att').value) || 0,
    projectManager:    document.getElementById('new-pm')
      .value.trim(),
    flagStatus:        'NONE',
    lastContactDate:   new Date().toISOString().split('T')[0]
  };
  if (!body.fullName || !body.cadetCode) {
    alert('Please enter a full name and cadet ID.'); return;
  }
  try {
    await post('/api/cadets', body);
    closeModal();
    loadCadets();
    loadDashboard();
  } catch (e) { alert('Failed to add cadet.'); }
}

// ── Flag modal ────────────────────────────────────────────────
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
  document.getElementById('flag-reason-row').style.display =
    action === 'restore' || action === 'orange' ? 'none' : 'block';
  document.getElementById('flag-notes-row').style.display =
    action === 'restore' ? 'block' : 'none';

  // Pre-fill triggered by
  const trigEl = document.getElementById('flag-triggered-by');
  if (trigEl && currentUser) trigEl.value = currentUser.fullName;

  document.getElementById('flag-modal').classList.add('open');
}

function closeFlagModal() {
  document.getElementById('flag-modal').classList.remove('open');
  currentFlagAction = null;
}

async function submitFlagAction() {
  if (!currentFlagAction) return;
  const { cadetId, action } = currentFlagAction;
  const triggeredBy =
    document.getElementById('flag-triggered-by').value.trim()
    || (currentUser ? currentUser.fullName : 'Admin');
  const reason =
    document.getElementById('flag-reason').value.trim()
    || 'No reason provided.';
  const notes =
    document.getElementById('flag-notes').value.trim() || '';
  try {
    if (action === 'yellow')
      await post(`/api/cadets/${cadetId}/flag/yellow`,
        { triggeredBy, reason });
    else if (action === 'orange')
      await post(`/api/cadets/${cadetId}/flag/orange`,
        { triggeredBy });
    else if (action === 'red')
      await post(`/api/cadets/${cadetId}/flag/red`,
        { triggeredBy, reason });
    else if (action === 'restore')
      await post(`/api/cadets/${cadetId}/restore`,
        { triggeredBy, notes });
    closeFlagModal();
    loadCadets();
    loadFlagView();
    loadDashboard();
  } catch (e) { alert('Action failed.'); }
}

// ── Helpers ───────────────────────────────────────────────────
function initials(name) {
  return (name || '').split(' ')
    .map(w => w[0]).join('').slice(0,2).toUpperCase();
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
        <div class="progress-fill ${cls}"
          style="width:${v}%"></div>
      </div>
      <span class="progress-pct">${v}%</span>
    </div>`;
}

// ── Init ──────────────────────────────────────────────────────
boot();