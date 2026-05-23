# Agent Server Tooling

Tools that consume the Microbot Agent Server's HTTP REST API (`http://127.0.0.1:8081`).

## What the Agent Server is

The `[M] Agent Server` plugin (bundled with Microbot core, not in this Hub repo) starts an HTTP server inside the running client that exposes game state, script lifecycle, and interaction primitives. 34 endpoints across:

- **Game state**: `/state`, `/skills`, `/inventory`, `/npcs`, `/objects`, `/widgets/*`, `/ground-items`, `/bank`, `/dialogue`, `/login`
- **Script lifecycle**: `/scripts`, `/scripts/start`, `/scripts/stop`, `/scripts/status`, `/scripts/results`
- **Game interactions**: `/walk`, `/widgets/click`, `/npcs interact`, `/objects interact`, `/inventory interact`

Canonical reference: [`docs/AGENT_SERVER.md`](../../docs/AGENT_SERVER.md) at repo root.

## What lives here

| Directory | Purpose |
|---|---|
| `dashboard/` | Single-page browser dashboard polling the Agent Server (Phase 1 MVP) |
| `HEALTHCHECK.md` | curl one-liners for connectivity + endpoint diagnostics |
| `CONFIG.md` | Locked-in Agent Server plugin config + rationale |

## Future tooling (deferred, see plan file)

| Phase | Trigger | What |
|---|---|---|
| 2 | Next non-trivial Plus plugin change | Regression test harness building on `src/test/java/.../ScriptLifecycleTest.java` |
| 3 | When Pete wants 9a-10p wall-clock automation | Daily orchestrator (PowerShell + Windows Task Scheduler) |
| 4 | When MiningPlus Phase A2 audit starts | Wiki-coord verifier (walks to each MineLocationOption, asserts arrival) |
| 5 | Post-v1.0.0 graduation | Bot-shape self-audit (uniformity metrics) |
| 6 | Specific automation use case (e.g. quest) | AI agent driving the bot (Claude over HTTP) |

## Quick start

```powershell
# 1. Enable the [M] Agent Server plugin in your Microbot client (toggle ON)

# 2. Smoke test connectivity
$TOKEN = Get-Content "$env:USERPROFILE\.runelite\.agent-token"
curl.exe -H "X-Agent-Token: $TOKEN" http://127.0.0.1:8081/state

# 3. Launch the dashboard (serve.ps1 handles auth + CORS via reverse proxy)
cd tools\agentserver\dashboard
.\serve.ps1
# Then open http://localhost:8088/ in your browser
```

If `curl` returns a connection refused error, the plugin isn't enabled. If it returns 404 on every path, the auth header is missing — `serve.ps1` handles this for you. See `HEALTHCHECK.md` for diagnostics.

## Why serve.ps1 (not just open the HTML file)?

The Agent Server doesn't set CORS headers, so browsers can't fetch it cross-origin from `file://` or any other port. `serve.ps1` runs as a tiny local web server that:

1. Serves the dashboard files (HTML/CSS/JS) over `http://localhost:8088`
2. Reverse-proxies `/api/*` requests to `http://127.0.0.1:8081/*`, attaching the `X-Agent-Token` header from `~/.runelite/.agent-token` server-side
3. Re-reads the token on every request, so plugin Reset / token rotation works without restarting

The browser only talks same-origin to serve.ps1. No CORS issues, no copy-pasting the token into the dashboard.
