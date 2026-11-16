package com.demobank.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves static HTML, CSS, and JS web assets for DemoBank.
 */
public final class StaticHandler implements HttpHandler {
    private final String webDir;

    public StaticHandler() {
        this(findWebDir());
    }

    public StaticHandler(String webDir) {
        this.webDir = webDir;
    }

    private static String findWebDir() {
        if (Files.isDirectory(Paths.get("web"))) return "web";
        if (Files.isDirectory(Paths.get("demobank/web"))) return "demobank/web";
        return "web";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            serveFile(exchange, Paths.get(webDir, "index.html"), "text/html; charset=UTF-8");
            return;
        }
        if ("/dashboard".equals(path) || "/dashboard.html".equals(path)) {
            serveFile(exchange, Paths.get(webDir, "dashboard.html"), "text/html; charset=UTF-8");
            return;
        }
        if ("/status.html".equals(path)) {
            serveFile(exchange, Paths.get(webDir, "status.html"), "text/html; charset=UTF-8");
            return;
        }

        String sub = path.startsWith("/") ? path.substring(1) : path;
        Path file = Paths.get(webDir, sub);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            HttpUtil.sendText(exchange, 404, "404 Not Found", "text/plain");
            return;
        }

        String mime = getMimeType(sub);
        serveFile(exchange, file, mime);
    }

    private void serveFile(HttpExchange exchange, Path file, String mime) throws IOException {
        if (!Files.exists(file)) {
            HttpUtil.sendText(exchange, 404, "404 Not Found", "text/plain");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
