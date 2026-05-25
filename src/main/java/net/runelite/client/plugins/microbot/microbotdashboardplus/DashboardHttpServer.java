package net.runelite.client.plugins.microbot.microbotdashboardplus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the dashboard. Started on plugin enable, stopped
 * on plugin disable.
 *
 * <p>Three responsibilities:
 * <ol>
 *   <li>Serve static files (HTML/CSS/JS). Bundled in JAR resources at
 *       {@code /net/runelite/client/plugins/microbot/microbotdashboardplus/dashboard/}.
 *       Dev-mode config can override to read from disk.</li>
 *   <li>Reverse-proxy {@code /api/*} to the Agent Server, attaching the
 *       {@code X-Agent-Token} header from {@code ~/.runelite/.agent-token}.</li>
 *   <li>Serve local CSV/JSONL files: {@code /watchdog-log},
 *       {@code /eventdismiss-log}, {@code /history/log} (GET + POST).</li>
 * </ol>
 *
 * <p>Same set of endpoints as the PowerShell {@code serve.ps1}. Java port runs
 * inside the Microbot JVM, so no external process needed.
 */
@Slf4j
public class DashboardHttpServer {

    private static final String RESOURCE_BASE =
            "/net/runelite/client/plugins/microbot/microbotdashboardplus/dashboard/";

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put(".html", "text/html; charset=utf-8");
        MIME_TYPES.put(".css",  "text/css; charset=utf-8");
        MIME_TYPES.put(".js",   "application/javascript; charset=utf-8");
        MIME_TYPES.put(".json", "application/json; charset=utf-8");
        MIME_TYPES.put(".svg",  "image/svg+xml");
        MIME_TYPES.put(".png",  "image/png");
        MIME_TYPES.put(".ico",  "image/x-icon");
    }

    private final MicrobotDashboardPlusConfig config;
    private HttpServer server;

    public DashboardHttpServer(MicrobotDashboardPlusConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        int port = config.serverPort();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        // Order doesn't matter: longest-match wins in createContext.
        server.createContext("/", this::handleStatic);
        server.createContext("/api/", this::handleApiProxy);
        server.createContext("/watchdog-log", this::handleWatchdogLog);
        server.createContext("/eventdismiss-log", this::handleEventDismissLog);
        server.createContext("/history/log", this::handleHistoryLog);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        log.info("MicrobotDashboardPlus HTTP server listening on http://127.0.0.1:{}/", port);
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop(1); // 1-sec grace period
                log.info("MicrobotDashboardPlus HTTP server stopped");
            } catch (Exception ex) {
                log.warn("Error stopping HTTP server: {}", ex.getMessage());
            } finally {
                server = null;
            }
        }
    }

    // ========================================================================
    // Static file handler
    // ========================================================================

    private void handleStatic(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";

            // Strip leading slash to get the relative file name.
            String relPath = path.startsWith("/") ? path.substring(1) : path;

            // Reject path-traversal attempts.
            if (relPath.contains("..") || relPath.startsWith("/")) {
                sendNotFound(exchange, "Invalid path: " + relPath);
                return;
            }

            byte[] content = loadResource(relPath);
            if (content == null) {
                sendNotFound(exchange, "Not found: " + relPath);
                return;
            }

            String contentType = mimeFor(relPath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        } catch (Exception ex) {
            log.warn("handleStatic error: {}", ex.getMessage());
            sendError(exchange, 500, "Server error: " + ex.getMessage());
        } finally {
            exchange.close();
        }
    }

    /**
     * Load a dashboard file. Dev-mode path wins if set + file exists; else
     * falls back to JAR classpath resources.
     */
    private byte[] loadResource(String relPath) {
        // Dev mode: read from disk
        String devPath = config.devModePath();
        if (devPath != null && !devPath.trim().isEmpty()) {
            Path filePath = Paths.get(devPath.trim(), relPath);
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                try {
                    return Files.readAllBytes(filePath);
                } catch (IOException ex) {
                    log.warn("Dev-mode read failed for {}: {}", filePath, ex.getMessage());
                    // Fall through to classpath
                }
            }
        }

        // Production: read from JAR classpath
        String resourcePath = RESOURCE_BASE + relPath;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            return baos.toByteArray();
        } catch (IOException ex) {
            log.warn("Classpath read failed for {}: {}", resourcePath, ex.getMessage());
            return null;
        }
    }

    private String mimeFor(String relPath) {
        int dot = relPath.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        String ext = relPath.substring(dot).toLowerCase();
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    // ========================================================================
    // Reverse proxy to Agent Server
    // ========================================================================

    private void handleApiProxy(HttpExchange exchange) throws IOException {
        try {
            String fullPath = exchange.getRequestURI().getPath();
            // Strip "/api/" prefix; the remainder is forwarded to the Agent Server.
            String upstreamPath = fullPath.startsWith("/api/")
                    ? fullPath.substring(4)  // keep the leading slash from /api/...
                    : "/";

            String query = exchange.getRequestURI().getQuery();
            if (query != null && !query.isEmpty()) {
                upstreamPath += "?" + query;
            }

            String token = readAgentToken();
            if (token == null) {
                sendError(exchange, 503, "Auth token not available at ~/.runelite/.agent-token");
                return;
            }

            String upstreamUrl = "http://127.0.0.1:" + config.agentServerPort() + upstreamPath;
            URL url = new URL(upstreamUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String method = exchange.getRequestMethod();
            conn.setRequestMethod(method);
            conn.setRequestProperty("X-Agent-Token", token);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);

            // Forward request body for write methods.
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
                conn.setDoOutput(true);
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
                try (InputStream in = exchange.getRequestBody();
                     OutputStream out = conn.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
            }

            int status = conn.getResponseCode();
            String upstreamContentType = conn.getContentType();
            byte[] body;
            try (InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                if (is == null) {
                    body = new byte[0];
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
                    body = baos.toByteArray();
                }
            }

            if (upstreamContentType != null) {
                exchange.getResponseHeaders().set("Content-Type", upstreamContentType);
            }
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (Exception ex) {
            log.warn("Proxy error: {}", ex.getMessage());
            sendError(exchange, 502, "Proxy error: " + ex.getMessage());
        } finally {
            exchange.close();
        }
    }

    /**
     * Read the auth token. Re-read every request so token rotation (plugin
     * Reset → file rewritten by Agent Server plugin) is picked up automatically.
     */
    private String readAgentToken() {
        Path tokenPath = Paths.get(System.getProperty("user.home"), ".runelite", ".agent-token");
        if (!Files.exists(tokenPath)) return null;
        try {
            return new String(Files.readAllBytes(tokenPath), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            log.warn("Token read failed: {}", ex.getMessage());
            return null;
        }
    }

    // ========================================================================
    // Watchdog log
    // ========================================================================

    private void handleWatchdogLog(HttpExchange exchange) throws IOException {
        sendFile(exchange,
                Paths.get(System.getProperty("user.home"), ".runelite", "microbot-watchdog.csv"),
                "text/csv; charset=utf-8");
    }

    // ========================================================================
    // EventDismiss log
    // ========================================================================

    private void handleEventDismissLog(HttpExchange exchange) throws IOException {
        sendFile(exchange,
                Paths.get(System.getProperty("user.home"), ".runelite", "eventdismissplus-events.csv"),
                "text/csv; charset=utf-8");
    }

    // ========================================================================
    // History log (GET + POST)
    // ========================================================================

    private void handleHistoryLog(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            Path historyPath = Paths.get(
                    System.getProperty("user.home"), ".runelite", "microbot-dashboard-history.jsonl");

            if ("GET".equals(method)) {
                sendFile(exchange, historyPath, "application/x-ndjson; charset=utf-8");
            } else if ("POST".equals(method)) {
                appendHistoryLine(exchange, historyPath);
            } else {
                sendError(exchange, 405, "Method not allowed: " + method);
            }
        } finally {
            exchange.close();
        }
    }

    private void appendHistoryLine(HttpExchange exchange, Path historyPath) throws IOException {
        // Read body
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = exchange.getRequestBody()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        }
        String body = baos.toString(StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) {
            sendError(exchange, 400, "empty body");
            return;
        }
        // Strip any embedded newlines so the line is truly a single JSONL entry.
        String cleanBody = body.replace("\r", "").replace("\n", " ");

        try {
            Files.createDirectories(historyPath.getParent());
            Files.write(historyPath,
                    (cleanBody + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            exchange.sendResponseHeaders(204, -1);
        } catch (IOException ex) {
            sendError(exchange, 500, "Write failed: " + ex.getMessage());
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Send a file as the response body. Returns empty 200 (not 404) if the
     * file doesn't exist - lets the dashboard render an "unavailable" state
     * rather than treating it as an error.
     */
    private void sendFile(HttpExchange exchange, Path path, String contentType) throws IOException {
        try {
            byte[] body;
            if (Files.exists(path)) {
                body = Files.readAllBytes(path);
            } else {
                body = new byte[0];
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (IOException ex) {
            sendError(exchange, 500, "Read failed: " + ex.getMessage());
        } finally {
            exchange.close();
        }
    }

    private void sendNotFound(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 404, message);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
