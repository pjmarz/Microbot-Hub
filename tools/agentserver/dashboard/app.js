/**
 * Microbot Agent Server Dashboard
 *
 * Polls /state, /skills, and /scripts on the Agent Server every N seconds and
 * renders a live view. No backend; all state lives in localStorage.
 *
 * Configurable via the Settings panel (gear icon). Defaults assume the Agent
 * Server runs on 127.0.0.1:8081 with no auth required.
 */

// ============================================================================
// Constants and state
// ============================================================================

// When served via `serve.ps1` (the supported path), the proxy lives at /api
// on the same origin. That handles CORS + the X-Agent-Token header for us.
// Pete only needs to override these in Settings if running without serve.ps1.
const DEFAULTS = {
    serverUrl: '/api',
    authToken: '',             // unused when behind the serve.ps1 proxy
    pollInterval: 5,           // seconds
};

const LS_KEYS = {
    serverUrl: 'microbot-agent-url',
    authToken: 'microbot-agent-token',
    pollInterval: 'microbot-agent-poll-interval',
    events: 'microbot-agent-events',
};

const RING_BUFFER_MAX = 10;

// Skill display order (matches in-game stat tab ordering).
const SKILL_ORDER = [
    'Attack', 'Hitpoints', 'Mining',
    'Strength', 'Agility', 'Smithing',
    'Defence', 'Herblore', 'Fishing',
    'Ranged', 'Thieving', 'Cooking',
    'Prayer', 'Crafting', 'Firemaking',
    'Magic', 'Fletching', 'Woodcutting',
    'Runecraft', 'Slayer', 'Farming',
    'Construction', 'Hunter',
];

// Read-or-default. Stale full-URL entries get migrated to the /api proxy default
// so anyone who upgraded mid-session doesn't stay stuck on the old direct fetch.
const storedUrl = localStorage.getItem(LS_KEYS.serverUrl);
let settings = {
    serverUrl: (storedUrl && storedUrl !== 'http://127.0.0.1:8081') ? storedUrl : DEFAULTS.serverUrl,
    authToken: localStorage.getItem(LS_KEYS.authToken) || DEFAULTS.authToken,
    pollInterval: parseInt(localStorage.getItem(LS_KEYS.pollInterval), 10) || DEFAULTS.pollInterval,
};

let pollTimer = null;
let initialSkills = null;      // First successful /skills snapshot, for delta calculation
let sessionStartMs = null;     // Wall-clock timestamp set with initialSkills (for XP/hr)
let lastState = null;          // Most recent /state, for diff-based event detection
let lastScripts = null;        // Most recent /scripts, same purpose
let lastLogin = null;          // Most recent /login (currentWorld, profile, etc.)
let lastInventoryCounts = null; // Map<itemName, totalQty> for diff
let events = [];               // Ring buffer
let stoppingClassNames = new Set();  // className -> in-flight Stop request (UI disable)
let startingClassNames = new Set();  // className -> in-flight Start request

// v0.3.0: rolling XP/hr — per-skill ring buffer of {ts, xp} samples.
// Windowed rate replaces session-average so BreakHandler breaks (no XP gained
// during a break) don't drag the rate down for the rest of the session.
const XP_RING_WINDOW_MS = 5 * 60 * 1000;   // 5 minutes
const xpRingBuffer = new Map();             // skillName -> [{ts, xp}]

// v0.3.0: known Plus plugins for the Quick Start panel. Hardcoded ordered list
// so the panel is stable and useful even when scripts API doesn't expose all
// of them as installed.
const PLUS_PLUGINS = [
    { name: 'Auto Mining Plus',     className: 'net.runelite.client.plugins.microbot.miningplus.AutoMiningPlusPlugin' },
    { name: 'Auto Smelting Plus',   className: 'net.runelite.client.plugins.microbot.smeltingplus.AutoSmeltingPlusPlugin' },
    { name: 'Auto Smithing Plus',   className: 'net.runelite.client.plugins.microbot.smithingplus.AutoSmithingPlusPlugin' },
    { name: 'Auto Woodcutting Plus',className: 'net.runelite.client.plugins.microbot.woodcuttingplus.AutoWoodcuttingPlusPlugin' },
    { name: 'Event Dismiss Plus',   className: 'net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusPlugin' },
];

// v0.3.0: random event NPC names to highlight in the Nearby NPCs panel.
// Matches the v0.1.x EventDismissPlus catalog plus Mime trio + Freaky Forester.
const RANDOM_EVENT_NPC_NAMES = new Set([
    'Genie',
    'Sandwich lady', 'Sandwich Lady',
    'Drunken Dwarf', 'Drunken dwarf',
    'Mysterious Old Man', 'Mysterious old man',
    'Bee keeper', 'Beekeeper',
    'Count Check',
    'Frog Prince', 'Frog Princess',
    'Rick Turpentine',
    'Dr Jekyll', 'Dr. Jekyll',
    'Niles', 'Miles', 'Giles',
    'Freaky Forester',
    'Prison Pete',
    'Strange Plant',  // also a GameObject; if it appears as NPC we still highlight
]);

// ============================================================================
// Init
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
    loadEvents();
    renderEvents();
    attachSettingsHandlers();
    attachStopScriptHandlers();
    startPolling();
});

// Event delegation for the Stop buttons in the Active Scripts panel. We
// re-render the table on every poll, so attaching individual listeners would
// leak. Delegate at the tbody level once.
function attachStopScriptHandlers() {
    const tbody = document.getElementById('scripts-body');
    tbody.addEventListener('click', async (ev) => {
        const btn = ev.target.closest('.stop-script-btn');
        if (!btn) return;
        const className = btn.dataset.classname;
        if (!className) return;
        await stopScript(className, btn);
    });

    // v0.3.0: same delegation pattern for the Plus Quick Start panel.
    const plusGrid = document.getElementById('plus-quickstart-grid');
    if (plusGrid) {
        plusGrid.addEventListener('click', async (ev) => {
            const btn = ev.target.closest('.start-plus-btn, .stop-script-btn');
            if (!btn) return;
            const className = btn.dataset.classname;
            if (!className) return;
            if (btn.classList.contains('start-plus-btn')) {
                await startScript(className, btn);
            } else {
                await stopScript(className, btn);
            }
        });
    }
}

async function stopScript(className, btn) {
    // Optimistic UI: disable the button while the request is in flight, mark
    // the className as "stopping" so re-renders during this tick keep it
    // disabled even if the script is still reported active.
    stoppingClassNames.add(className);
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Stopping...';
    }
    try {
        const headers = { 'Content-Type': 'application/json' };
        if (settings.authToken) headers['X-Agent-Token'] = settings.authToken;
        const resp = await fetch(settings.serverUrl + '/scripts/stop', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ className: className }),
        });
        if (resp.ok) {
            pushEvent(`Stop requested: ${stripHtmlTags(className.split('.').pop())}`);
        } else {
            pushEvent(`Stop FAILED (HTTP ${resp.status}): ${className.split('.').pop()}`);
        }
    } catch (err) {
        pushEvent(`Stop ERROR: ${err.message}`);
    } finally {
        setTimeout(() => {
            stoppingClassNames.delete(className);
            poll();
        }, 1500);
    }
}

/**
 * v0.3.0: POST /scripts/start with a className. Mirrors stopScript flow.
 */
async function startScript(className, btn) {
    startingClassNames.add(className);
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Starting...';
    }
    try {
        const headers = { 'Content-Type': 'application/json' };
        if (settings.authToken) headers['X-Agent-Token'] = settings.authToken;
        const resp = await fetch(settings.serverUrl + '/scripts/start', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ className: className }),
        });
        if (resp.ok) {
            pushEvent(`Start requested: ${className.split('.').pop()}`);
        } else {
            pushEvent(`Start FAILED (HTTP ${resp.status}): ${className.split('.').pop()}`);
        }
    } catch (err) {
        pushEvent(`Start ERROR: ${err.message}`);
    } finally {
        setTimeout(() => {
            startingClassNames.delete(className);
            poll();
        }, 1500);
    }
}

// ============================================================================
// Settings panel
// ============================================================================

function attachSettingsHandlers() {
    const panel = document.getElementById('settings-panel');
    const btn = document.getElementById('settings-btn');
    const closeBtn = document.getElementById('close-settings');
    const saveBtn = document.getElementById('save-settings');
    const resetBtn = document.getElementById('reset-events');

    // Pre-fill inputs from current settings.
    document.getElementById('server-url').value = settings.serverUrl;
    document.getElementById('auth-token').value = settings.authToken;
    document.getElementById('poll-interval').value = settings.pollInterval;

    btn.addEventListener('click', () => panel.classList.toggle('hidden'));
    closeBtn.addEventListener('click', () => panel.classList.add('hidden'));

    saveBtn.addEventListener('click', () => {
        settings.serverUrl = document.getElementById('server-url').value.trim();
        settings.authToken = document.getElementById('auth-token').value.trim();
        settings.pollInterval = parseInt(document.getElementById('poll-interval').value, 10) || DEFAULTS.pollInterval;

        localStorage.setItem(LS_KEYS.serverUrl, settings.serverUrl);
        localStorage.setItem(LS_KEYS.authToken, settings.authToken);
        localStorage.setItem(LS_KEYS.pollInterval, settings.pollInterval.toString());

        // Restart polling with new settings.
        stopPolling();
        initialSkills = null;          // Reset XP delta baseline
        sessionStartMs = null;         // Reset XP/hr baseline
        lastState = null;
        lastScripts = null;
        lastInventoryCounts = null;
        pushEvent('Settings updated');
        startPolling();
        panel.classList.add('hidden');
    });

    resetBtn.addEventListener('click', () => {
        if (!confirm('Clear the event log?')) return;
        events = [];
        localStorage.removeItem(LS_KEYS.events);
        renderEvents();
    });
}

// ============================================================================
// Polling loop
// ============================================================================

function startPolling() {
    poll();                        // Immediate fire
    pollTimer = setInterval(poll, settings.pollInterval * 1000);
}

function stopPolling() {
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = null;
}

async function poll() {
    try {
        // Fetch all endpoints in parallel. /login carries the world number,
        // profile name, member status, and login duration -- none of which
        // are in /state. /inventory drives the inventory panel + diff log.
        // /npcs drives the Nearby NPCs panel (v0.3.0).
        const [state, skills, scripts, login, inventory, npcs] = await Promise.all([
            fetchEndpoint('/state'),
            fetchEndpoint('/skills'),
            fetchEndpoint('/scripts'),
            fetchEndpoint('/login'),
            fetchEndpoint('/inventory').catch(() => null),  // tolerate logged-out
            fetchEndpoint('/npcs').catch(() => null),
        ]);

        setConnected(true);
        updateLastPoll();

        // Render each section. Renderers are defensive — they handle missing
        // or malformed data without crashing.
        renderPlayer(state, login);
        renderScripts(scripts);
        renderPlusQuickStart(scripts);     // v0.3.0
        renderSkills(skills);
        renderInventory(inventory);
        renderNearbyNpcs(npcs);            // v0.3.0

        // Diff against previous tick to surface notable changes as events.
        diffAndPushEvents(state, scripts, login, inventory);

        lastState = state;
        lastScripts = scripts;
        lastLogin = login;
    } catch (err) {
        setConnected(false, err.message);
    }

    // v0.3.0: watchdog status fetched separately from a non-proxied endpoint
    // (serve.ps1 serves the CSV directly). Doesn't affect connection status to
    // Agent Server if it fails.
    pollWatchdog();
}

async function fetchEndpoint(path) {
    const headers = {};
    if (settings.authToken) {
        // Verified 2026-05-23: Agent Server uses X-Agent-Token header (not
        // Authorization: Bearer). Without this header, every path 404s.
        headers['X-Agent-Token'] = settings.authToken;
    }

    const resp = await fetch(settings.serverUrl + path, { headers });
    if (!resp.ok) {
        throw new Error(`HTTP ${resp.status} on ${path}`);
    }
    return await resp.json();
}

// ============================================================================
// Connection status
// ============================================================================

function setConnected(connected, reason = '') {
    const el = document.getElementById('connection-status');
    if (connected) {
        el.textContent = 'Connected';
        el.className = 'status-connected';
    } else {
        el.textContent = `Disconnected${reason ? ': ' + reason : ''}`;
        el.className = 'status-disconnected';
    }
}

function updateLastPoll() {
    const t = new Date().toLocaleTimeString();
    document.getElementById('last-poll').textContent = `Last poll: ${t}`;
}

// ============================================================================
// Renderers
// ============================================================================

function renderPlayer(state, login) {
    // Verified shapes (2026-05-23):
    //   /state: { loggedIn, gameState, scriptsPaused, player: { name,
    //     combatLevel, position: {x, y, plane}, animating, animationId,
    //     moving, interacting, ... } }
    //   /login: { loggedIn, gameState, loginAttemptActive, loginDurationMs,
    //     activeProfile: { name, isMember }, currentWorld }
    if (!state) return;

    const player = state.player || {};
    const name = player.name || state.playerName || '--';
    const combat = player.combatLevel != null ? player.combatLevel : '--';
    const loggedIn = state.loggedIn != null ? (state.loggedIn ? 'yes' : 'no') : '--';
    const gameState = state.gameState || '--';
    const world = login && login.currentWorld != null ? login.currentWorld : '--';

    let profile = '--';
    if (login && login.activeProfile) {
        const ap = login.activeProfile;
        profile = ap.name + (ap.isMember ? ' (P2P)' : ' (F2P)');
    }

    const session = (login && login.loginDurationMs)
        ? formatRuntime(login.loginDurationMs)
        : '--';

    const pos = player.position || state.worldPoint || state.position;
    const position = pos ? `(${pos.x}, ${pos.y}, ${pos.plane})` : '--';

    // Compose a one-line animation/state summary from the boolean flags the
    // API actually exposes.
    const flags = [];
    if (player.animating) flags.push(`anim ${player.animationId}`);
    if (player.moving) flags.push('moving');
    if (player.interacting) flags.push('interacting');
    if (state.scriptsPaused) flags.push('PAUSED');
    const animation = flags.length ? flags.join(', ') : 'idle';

    document.getElementById('player-name').textContent = name;
    document.getElementById('player-combat').textContent = combat;
    document.getElementById('player-logged-in').textContent = loggedIn;
    document.getElementById('player-game-state').textContent = gameState;
    document.getElementById('player-world').textContent = world;
    document.getElementById('player-profile').textContent = profile;
    document.getElementById('player-session').textContent = session;
    document.getElementById('player-position').textContent = position;
    document.getElementById('player-animation').textContent = animation;
}

function renderScripts(scriptsResponse) {
    const tbody = document.getElementById('scripts-body');
    // Verified /scripts shape (2026-05-23): { count, scripts: [{ name,
    // className, active, enabled }] }. Some Plus plugin names arrive
    // with embedded HTML colour tags (e.g. "<html>[<font color=...>...]").
    const list = scriptsResponse && Array.isArray(scriptsResponse.scripts)
        ? scriptsResponse.scripts
        : (Array.isArray(scriptsResponse) ? scriptsResponse : []);

    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty">No scripts data</td></tr>';
        return;
    }

    const active = list.filter(s => s.active);

    if (active.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty">No active scripts</td></tr>';
        return;
    }

    tbody.innerHTML = active.map(s => {
        const name = stripHtmlTags(s.name || s.className || '(unnamed)');
        const status = s.active ? 'running' : (s.enabled ? 'enabled' : '--');
        const runtime = s.runtimeMs ? formatRuntime(s.runtimeMs) : '--';
        const className = s.className || '';
        const isStopping = stoppingClassNames.has(className);
        const buttonHtml = className
            ? `<button class="stop-script-btn" data-classname="${escapeHtml(className)}" ${isStopping ? 'disabled' : ''}>${isStopping ? 'Stopping...' : 'Stop'}</button>`
            : '<span class="muted">--</span>';
        return `<tr>
            <td>${escapeHtml(name)}</td>
            <td>${escapeHtml(status)}</td>
            <td>${escapeHtml(runtime)}</td>
            <td class="action-cell">${buttonHtml}</td>
        </tr>`;
    }).join('');
}

/**
 * Some plugin names embed HTML tags for in-game color rendering
 * (e.g. "<html>[<font color=green>D</font>] BreakHandler"). Strip them
 * for the dashboard's plain-text display.
 */
function stripHtmlTags(s) {
    if (!s) return '';
    return s.replace(/<[^>]+>/g, '').trim();
}

function renderSkills(skills) {
    const tbody = document.getElementById('skills-body');
    if (!skills) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">No skills data</td></tr>';
        return;
    }

    // Capture the first snapshot as our delta baseline + session-start
    // timestamp. Both reset on settings save.
    if (initialSkills === null) {
        initialSkills = normalizeSkills(skills);
        sessionStartMs = Date.now();
    }

    const current = normalizeSkills(skills);
    const now = Date.now();

    // Push samples into the rolling window (v0.3.0).
    for (const skill of SKILL_ORDER) {
        const cur = current[skill];
        if (!cur) continue;
        pushXpSample(skill, now, cur.xp);
    }

    tbody.innerHTML = SKILL_ORDER.map(skill => {
        const cur = current[skill];
        if (!cur) return '';   // Skill not present (e.g. older API response)
        const initial = initialSkills[skill] || cur;
        const delta = cur.xp - initial.xp;
        const deltaClass = delta > 0 ? 'delta-positive' : 'delta-zero';
        const deltaStr = delta > 0 ? `+${delta.toLocaleString()}` : '0';

        // v0.3.0: rolling 5-min XP/hr rate. Falls back to session-average
        // when the window has < 30 sec of samples (so the first ticks after
        // page load don't show wildly inaccurate numbers).
        const xpPerHour = getRollingXpRate(skill, XP_RING_WINDOW_MS, delta, now);
        const rateClass = xpPerHour > 0 ? 'rate-cell' : 'rate-zero';
        const rateStr = xpPerHour > 0 ? xpPerHour.toLocaleString() : '0';

        return `<tr>
            <td>${skill}</td>
            <td>${cur.level}</td>
            <td>${cur.xp.toLocaleString()}</td>
            <td class="${deltaClass}">${deltaStr}</td>
            <td class="${rateClass}">${rateStr}</td>
        </tr>`;
    }).join('');
}

/**
 * v0.3.0: push an XP sample for one skill into the rolling ring buffer,
 * trimming any sample older than XP_RING_WINDOW_MS.
 */
function pushXpSample(skillName, ts, xp) {
    let buf = xpRingBuffer.get(skillName);
    if (!buf) {
        buf = [];
        xpRingBuffer.set(skillName, buf);
    }
    buf.push({ ts, xp });
    // Trim from the front while the oldest sample is outside the window.
    const cutoff = ts - XP_RING_WINDOW_MS;
    while (buf.length > 0 && buf[0].ts < cutoff) {
        buf.shift();
    }
}

/**
 * Compute XP/hr from the rolling window. Returns 0 if no samples or no XP gained.
 * Falls back to session-average when the window has < 30 sec of data so early
 * polls don't extrapolate from a sample size of 2.
 *
 * @param sessionDelta XP gained since page load (for fallback path)
 * @param nowMs current timestamp
 */
function getRollingXpRate(skillName, windowMs, sessionDelta, nowMs) {
    const buf = xpRingBuffer.get(skillName);
    if (!buf || buf.length < 2) {
        // Not enough samples — fall back to session-average.
        if (sessionStartMs && sessionDelta > 0) {
            const sessionHours = Math.max((nowMs - sessionStartMs) / 3600000, 1 / 3600);
            return Math.round(sessionDelta / sessionHours);
        }
        return 0;
    }
    const oldest = buf[0];
    const newest = buf[buf.length - 1];
    const spanMs = newest.ts - oldest.ts;
    if (spanMs < 30000) {
        // Window too small (< 30 sec) — fall back to session-average to avoid
        // extrapolating a 5-sec sample to "XP/hr."
        if (sessionStartMs && sessionDelta > 0) {
            const sessionHours = Math.max((nowMs - sessionStartMs) / 3600000, 1 / 3600);
            return Math.round(sessionDelta / sessionHours);
        }
        return 0;
    }
    const xpDelta = newest.xp - oldest.xp;
    if (xpDelta <= 0) return 0;
    return Math.round(xpDelta / (spanMs / 3600000));
}

// ============================================================================
// Inventory rendering + diff
// ============================================================================

/**
 * Verified /inventory shape (2026-05-25):
 *   { count, capacity, freeSlots, full, items: [{ id, name, quantity,
 *     slot, stackable, noted, actions }] }
 *
 * We aggregate by name (the same coal occupies slots 0-3 separately for
 * unstackable items; we sum quantities). Noted items get a separate badge
 * since they behave differently (banking only, no use-on-furnace etc.).
 */
function renderInventory(inventory) {
    const grid = document.getElementById('inventory-grid');
    const summary = document.getElementById('inventory-summary');

    if (!inventory || !inventory.items) {
        grid.innerHTML = '<div class="empty">No inventory data (logged out?)</div>';
        summary.textContent = '--';
        return;
    }

    // Aggregate by (name + noted) so noted/unnoted versions of the same item
    // render as separate tiles.
    const counts = new Map();
    for (const item of inventory.items) {
        const key = item.noted ? `${item.name} (noted)` : item.name;
        const prev = counts.get(key) || { name: item.name, noted: item.noted, qty: 0 };
        prev.qty += (item.quantity || 1);
        counts.set(key, prev);
    }

    if (counts.size === 0) {
        grid.innerHTML = '<div class="empty">Inventory empty</div>';
    } else {
        // Sort tiles alphabetically for stable layout — diff log surfaces
        // changes; the grid is for at-a-glance state.
        const sorted = Array.from(counts.entries()).sort((a, b) => a[0].localeCompare(b[0]));
        grid.innerHTML = sorted.map(([key, info]) => {
            const cls = info.noted ? 'inv-item inv-noted' : 'inv-item';
            return `<div class="${cls}">
                <span class="inv-name" title="${escapeHtml(key)}">${escapeHtml(info.name)}${info.noted ? ' (n)' : ''}</span>
                <span class="inv-qty">${info.qty.toLocaleString()}</span>
            </div>`;
        }).join('');
    }

    summary.textContent = `${inventory.count}/${inventory.capacity} (${inventory.freeSlots} free)`;
    return counts;
}

// ============================================================================
// Watchdog status (v0.3.0)
// ============================================================================

/**
 * Fetch the watchdog CSV log from serve.ps1's /watchdog-log endpoint.
 * Parses the CSV, extracts last event + restart count + last restart, renders.
 * Empty file or missing endpoint → "unavailable" state (watchdog not running
 * or no events yet). Doesn't affect Agent Server connection status.
 */
async function pollWatchdog() {
    const summary = document.getElementById('watchdog-summary');
    const status = document.getElementById('watchdog-status');
    const lastEvent = document.getElementById('watchdog-last-event');
    const lastRestart = document.getElementById('watchdog-last-restart');
    const totalRestarts = document.getElementById('watchdog-total-restarts');
    if (!summary) return;  // Section not present (shouldn't happen but defensive)

    try {
        // Use absolute /watchdog-log (NOT prefixed with /api). serve.ps1 owns
        // this path; the Agent Server doesn't.
        const resp = await fetch('/watchdog-log');
        if (!resp.ok) {
            renderWatchdogUnavailable();
            return;
        }
        const text = await resp.text();
        if (!text.trim()) {
            renderWatchdogUnavailable();
            return;
        }
        renderWatchdog(parseWatchdogCsv(text));
    } catch {
        renderWatchdogUnavailable();
    }
}

function parseWatchdogCsv(text) {
    // Format: timestamp,reason,details (header + N data rows).
    const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
    if (lines.length < 2) return { rows: [] };  // header only

    const rows = [];
    for (let i = 1; i < lines.length; i++) {
        // Simple CSV parse — our writer escapes commas in details to ';'.
        const parts = lines[i].split(',');
        if (parts.length < 2) continue;
        rows.push({
            timestamp: parts[0],
            reason: parts[1],
            details: parts.slice(2).join(','),
        });
    }
    return { rows };
}

function renderWatchdog({ rows }) {
    const summary = document.getElementById('watchdog-summary');
    const status = document.getElementById('watchdog-status');
    const lastEvent = document.getElementById('watchdog-last-event');
    const lastRestart = document.getElementById('watchdog-last-restart');
    const totalRestarts = document.getElementById('watchdog-total-restarts');

    if (!rows || rows.length === 0) {
        renderWatchdogUnavailable();
        return;
    }

    // Latest entry = last row in the chronologically-appended CSV.
    const newest = rows[rows.length - 1];
    const newestAge = relativeTime(newest.timestamp);

    // Restart rows = anything that is not WATCHDOG_START.
    const restartRows = rows.filter(r => r.reason !== 'WATCHDOG_START');
    const latestRestart = restartRows.length ? restartRows[restartRows.length - 1] : null;

    // Status determination:
    //   - newest row is WATCHDOG_START + no restarts since → OK
    //   - any restart in last 5 min → warn
    //   - any restart in last 1 hour → warn
    //   - else OK
    const fiveMinAgo = Date.now() - 5 * 60 * 1000;
    const oneHourAgo = Date.now() - 60 * 60 * 1000;
    const recentRestart = latestRestart && new Date(latestRestart.timestamp).getTime() > fiveMinAgo;
    const restartInLastHour = latestRestart && new Date(latestRestart.timestamp).getTime() > oneHourAgo;

    let statusText, statusClass;
    if (recentRestart) {
        statusText = `Recent restart (${relativeTime(latestRestart.timestamp)})`;
        statusClass = 'watchdog-status-warn';
    } else if (restartInLastHour) {
        statusText = `Stable (last restart ${relativeTime(latestRestart.timestamp)})`;
        statusClass = 'watchdog-status-warn';
    } else {
        statusText = 'Stable';
        statusClass = 'watchdog-status-ok';
    }

    status.textContent = statusText;
    status.className = statusClass;
    lastEvent.textContent = `${newest.reason} (${newestAge})`;
    lastRestart.textContent = latestRestart
        ? `${latestRestart.reason} (${relativeTime(latestRestart.timestamp)})`
        : 'never';
    totalRestarts.textContent = restartRows.length.toString();
    summary.textContent = `${restartRows.length} restart${restartRows.length === 1 ? '' : 's'}`;
}

function renderWatchdogUnavailable() {
    const summary = document.getElementById('watchdog-summary');
    const status = document.getElementById('watchdog-status');
    const lastEvent = document.getElementById('watchdog-last-event');
    const lastRestart = document.getElementById('watchdog-last-restart');
    const totalRestarts = document.getElementById('watchdog-total-restarts');
    if (!summary) return;
    summary.textContent = 'not running';
    status.textContent = 'unavailable';
    status.className = 'watchdog-status-unavailable';
    lastEvent.textContent = '--';
    lastRestart.textContent = '--';
    totalRestarts.textContent = '--';
}

/**
 * Convert an ISO-8601 timestamp to a human "5m ago" string.
 */
function relativeTime(isoStr) {
    const ts = new Date(isoStr).getTime();
    if (isNaN(ts)) return 'unknown';
    const diff = Date.now() - ts;
    if (diff < 60000) return `${Math.round(diff / 1000)}s ago`;
    if (diff < 3600000) return `${Math.round(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.round(diff / 3600000)}h ago`;
    return `${Math.round(diff / 86400000)}d ago`;
}

// ============================================================================
// Plus Quick Start (v0.3.0)
// ============================================================================

/**
 * Render the Plus plugins panel. Looks up each PLUS_PLUGINS entry in the
 * scripts response by className. Renders a row with Start (inactive) or Stop
 * (active) button per plugin. Not-installed plugins render disabled.
 */
function renderPlusQuickStart(scriptsResponse) {
    const grid = document.getElementById('plus-quickstart-grid');
    if (!grid) return;

    const list = scriptsResponse && Array.isArray(scriptsResponse.scripts)
        ? scriptsResponse.scripts
        : [];
    const byClassName = new Map();
    for (const s of list) {
        if (s.className) byClassName.set(s.className, s);
    }

    grid.innerHTML = PLUS_PLUGINS.map(p => {
        const installed = byClassName.has(p.className);
        const script = byClassName.get(p.className);
        const active = installed && script.active;
        const stopping = stoppingClassNames.has(p.className);
        const starting = startingClassNames.has(p.className);

        let buttonHtml;
        if (!installed) {
            buttonHtml = '<button class="start-plus-btn" disabled title="Plugin not present">Not installed</button>';
        } else if (active) {
            buttonHtml = `<button class="stop-script-btn" data-classname="${escapeHtml(p.className)}" ${stopping ? 'disabled' : ''}>${stopping ? 'Stopping...' : 'Stop'}</button>`;
        } else {
            buttonHtml = `<button class="start-plus-btn" data-classname="${escapeHtml(p.className)}" ${starting ? 'disabled' : ''}>${starting ? 'Starting...' : 'Start'}</button>`;
        }

        const rowClass = active ? 'plus-row plus-active' : (installed ? 'plus-row' : 'plus-row plus-not-installed');
        return `<div class="${rowClass}">
            <span class="plus-name" title="${escapeHtml(p.className)}">${escapeHtml(p.name)}</span>
            ${buttonHtml}
        </div>`;
    }).join('');
}

// ============================================================================
// Nearby NPCs (v0.3.0)
// ============================================================================

/**
 * Render the Nearby NPCs panel. Aggregates by name, sorts by distance (closest
 * first), highlights random-event NPCs from the EventDismissPlus catalog.
 */
function renderNearbyNpcs(npcsResponse) {
    const list = document.getElementById('nearby-npcs-list');
    const summary = document.getElementById('nearby-npcs-summary');
    if (!list || !summary) return;

    if (!npcsResponse || !Array.isArray(npcsResponse.npcs) || npcsResponse.npcs.length === 0) {
        list.innerHTML = '<div class="empty">No NPCs in scene</div>';
        summary.textContent = '0';
        return;
    }

    // Aggregate by name; track minimum distance + count.
    const byName = new Map();
    for (const npc of npcsResponse.npcs) {
        if (!npc.name) continue;
        const existing = byName.get(npc.name);
        if (existing) {
            existing.count += 1;
            if (npc.distance != null && npc.distance < existing.minDistance) {
                existing.minDistance = npc.distance;
            }
        } else {
            byName.set(npc.name, {
                name: npc.name,
                count: 1,
                minDistance: npc.distance != null ? npc.distance : Infinity,
                combatLevel: npc.combatLevel || 0,
            });
        }
    }

    const rows = Array.from(byName.values()).sort((a, b) => a.minDistance - b.minDistance);

    list.innerHTML = rows.map(npc => {
        const isEvent = RANDOM_EVENT_NPC_NAMES.has(npc.name);
        const rowClass = isEvent ? 'npc-row npc-random-event' : 'npc-row';
        const distStr = npc.minDistance === Infinity ? '?' : Math.round(npc.minDistance);
        const cbStr = npc.combatLevel > 0 ? `cb ${npc.combatLevel}` : '';
        const meta = [`x${npc.count}`, cbStr, `${distStr}t`].filter(Boolean).join(' &middot; ');
        return `<div class="${rowClass}">
            <span class="npc-name">${isEvent ? '⚠ ' : ''}${escapeHtml(npc.name)}</span>
            <span class="npc-meta">${meta}</span>
        </div>`;
    }).join('');

    const eventCount = rows.filter(r => RANDOM_EVENT_NPC_NAMES.has(r.name)).length;
    summary.textContent = eventCount > 0
        ? `${npcsResponse.count} (${eventCount} random event!)`
        : `${npcsResponse.count}`;
}

/**
 * Compare two count maps and return a list of event strings.
 * Each entry: { type: 'picked-up'|'used-dropped', name, qty }
 */
function diffInventoryCounts(prev, cur) {
    const events = [];
    if (!prev || !cur) return events;

    const allKeys = new Set([...prev.keys(), ...cur.keys()]);
    for (const key of allKeys) {
        const p = prev.get(key);
        const c = cur.get(key);
        const prevQty = p ? p.qty : 0;
        const curQty = c ? c.qty : 0;
        const delta = curQty - prevQty;
        if (delta === 0) continue;

        const displayName = c ? key : (p ? key : 'Unknown');
        if (delta > 0) {
            events.push(`Picked up: +${delta} ${displayName}`);
        } else {
            events.push(`Used/dropped: ${delta} ${displayName}`);
        }
    }
    return events;
}

/**
 * Verified /skills shape (2026-05-23):
 *   { count, totalLevel, skills: [{ name, level, boostedLevel, xp }] }
 *
 * Returns { Attack: {level, xp}, ... } for easy lookup.
 */
function normalizeSkills(skillsResponse) {
    const out = {};
    const arr = skillsResponse && Array.isArray(skillsResponse.skills)
        ? skillsResponse.skills
        : (Array.isArray(skillsResponse) ? skillsResponse : []);
    for (const s of arr) {
        const name = s.name || s.skill;
        if (!name) continue;
        out[name] = {
            level: s.level || s.realLevel || 0,
            xp: s.xp || s.experience || 0,
        };
    }
    return out;
}

// ============================================================================
// Event ring buffer
// ============================================================================

function pushEvent(text) {
    const ev = {
        timestamp: new Date().toLocaleTimeString(),
        text: text,
    };
    events.unshift(ev);
    if (events.length > RING_BUFFER_MAX) events.length = RING_BUFFER_MAX;
    localStorage.setItem(LS_KEYS.events, JSON.stringify(events));
    renderEvents();
}

function loadEvents() {
    try {
        const raw = localStorage.getItem(LS_KEYS.events);
        if (raw) events = JSON.parse(raw);
    } catch {
        events = [];
    }
}

function renderEvents() {
    const ul = document.getElementById('events-list');
    if (events.length === 0) {
        ul.innerHTML = '<li class="empty">No events yet</li>';
        return;
    }
    ul.innerHTML = events.map(ev => {
        return `<li><span class="timestamp">${escapeHtml(ev.timestamp)}</span><span class="event-text">${escapeHtml(ev.text)}</span></li>`;
    }).join('');
}

/**
 * Diff the latest state + scripts against the previous tick to spot
 * meaningful changes (login/logout, script start/stop, world hop). Each
 * change becomes a one-line event in the ring buffer.
 */
function diffAndPushEvents(state, scriptsResponse, login, inventory) {
    if (lastState) {
        if (state.loggedIn !== lastState.loggedIn) {
            pushEvent(state.loggedIn ? 'Logged in' : 'Logged out');
        }
        if (state.scriptsPaused !== lastState.scriptsPaused) {
            pushEvent(state.scriptsPaused ? 'Scripts paused' : 'Scripts resumed');
        }
        if (state.gameState !== lastState.gameState && state.gameState && lastState.gameState) {
            pushEvent(`Game state: ${lastState.gameState} -> ${state.gameState}`);
        }
    }

    if (lastLogin && login && lastLogin.currentWorld !== login.currentWorld
            && lastLogin.currentWorld && login.currentWorld) {
        pushEvent(`World hop: ${lastLogin.currentWorld} -> ${login.currentWorld}`);
    }

    const cur = scriptsResponse && Array.isArray(scriptsResponse.scripts)
        ? scriptsResponse.scripts
        : (Array.isArray(scriptsResponse) ? scriptsResponse : null);
    const prev = lastScripts && Array.isArray(lastScripts.scripts)
        ? lastScripts.scripts
        : (Array.isArray(lastScripts) ? lastScripts : null);

    if (cur && prev) {
        const wasActive = new Set(prev.filter(s => s.active).map(s => stripHtmlTags(s.name || s.className)));
        const isActive = new Set(cur.filter(s => s.active).map(s => stripHtmlTags(s.name || s.className)));

        for (const n of isActive) {
            if (!wasActive.has(n)) pushEvent(`Started: ${n}`);
        }
        for (const n of wasActive) {
            if (!isActive.has(n)) pushEvent(`Stopped: ${n}`);
        }
    }

    // Inventory diff: build current counts, diff against lastInventoryCounts,
    // push events for each item that changed. First tick after page load
    // establishes the baseline without producing spurious events.
    if (inventory && inventory.items) {
        const curCounts = new Map();
        for (const item of inventory.items) {
            const key = item.noted ? `${item.name} (noted)` : item.name;
            const prev = curCounts.get(key) || { name: item.name, noted: item.noted, qty: 0 };
            prev.qty += (item.quantity || 1);
            curCounts.set(key, prev);
        }
        if (lastInventoryCounts !== null) {
            const invEvents = diffInventoryCounts(lastInventoryCounts, curCounts);
            for (const e of invEvents) pushEvent(e);
        }
        lastInventoryCounts = curCounts;
    }
}

// ============================================================================
// Utilities
// ============================================================================

function formatRuntime(ms) {
    const totalSec = Math.floor(ms / 1000);
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    if (h > 0) return `${h}h ${m}m ${s}s`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
}

function escapeHtml(s) {
    if (s == null) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
