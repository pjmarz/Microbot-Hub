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
        // Trigger an immediate re-poll so the table reflects the change faster
        // than the next scheduled tick.
        setTimeout(() => {
            stoppingClassNames.delete(className);
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
        // are in /state. /inventory drives the new inventory panel + diff log.
        const [state, skills, scripts, login, inventory] = await Promise.all([
            fetchEndpoint('/state'),
            fetchEndpoint('/skills'),
            fetchEndpoint('/scripts'),
            fetchEndpoint('/login'),
            fetchEndpoint('/inventory').catch(() => null),  // tolerate logged-out
        ]);

        setConnected(true);
        updateLastPoll();

        // Render each section. Renderers are defensive — they handle missing
        // or malformed data without crashing.
        renderPlayer(state, login);
        renderScripts(scripts);
        renderSkills(skills);
        renderInventory(inventory);

        // Diff against previous tick to surface notable changes as events.
        diffAndPushEvents(state, scripts, login, inventory);

        lastState = state;
        lastScripts = scripts;
        lastLogin = login;
    } catch (err) {
        setConnected(false, err.message);
    }
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
    // timestamp for the XP/hr calculation. Both reset on settings save.
    if (initialSkills === null) {
        initialSkills = normalizeSkills(skills);
        sessionStartMs = Date.now();
    }

    const current = normalizeSkills(skills);
    const elapsedHours = sessionStartMs
        ? Math.max((Date.now() - sessionStartMs) / 3600000, 1 / 3600)  // floor at 1 sec to avoid div-by-zero
        : 1 / 3600;

    tbody.innerHTML = SKILL_ORDER.map(skill => {
        const cur = current[skill];
        if (!cur) return '';   // Skill not present (e.g. older API response)
        const initial = initialSkills[skill] || cur;
        const delta = cur.xp - initial.xp;
        const deltaClass = delta > 0 ? 'delta-positive' : 'delta-zero';
        const deltaStr = delta > 0 ? `+${delta.toLocaleString()}` : '0';

        // XP/hr: extrapolate from session delta. Less reliable in the first
        // ~30 seconds of a session but stabilizes quickly.
        const xpPerHour = delta > 0 ? Math.round(delta / elapsedHours) : 0;
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
