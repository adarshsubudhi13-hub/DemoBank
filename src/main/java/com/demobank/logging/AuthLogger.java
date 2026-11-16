package com.demobank.logging;

import com.demobank.config.Config;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe single-writer asynchronous logger appending JSON lines to auth.log.
 */
public final class AuthLogger {
    private final BlockingQueue<AuthLogEntry> queue = new LinkedBlockingQueue<>(20000);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean logHealthy = new AtomicBoolean(true);
    private final Gson gson = new Gson();
    private final String logPath;
    private final Thread writerThread;
    private BufferedWriter writer;

    public AuthLogger() {
        this(Config.getAuthLogPath());
    }

    public AuthLogger(String logPath) {
        this.logPath = logPath;
        openWriter();
        this.writerThread = new Thread(this::processQueue, "auth-logger-thread");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    public void log(AuthLogEntry entry) {
        if (!queue.offer(entry)) {
            System.err.println("AuthLogger queue full; dropped entry: " + entry);
        }
    }

    public boolean isHealthy() {
        return logHealthy.get();
    }

    private synchronized void openWriter() {
        try {
            Path path = Paths.get(logPath);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            this.writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(path.toFile(), true), StandardCharsets.UTF_8));
            logHealthy.set(true);
        } catch (IOException e) {
            logHealthy.set(false);
            System.err.println("Failed to open auth.log for append: " + e.getMessage());
        }
    }

    private void processQueue() {
        while (running.get() || !queue.isEmpty()) {
            try {
                AuthLogEntry entry = queue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    writeLine(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void writeLine(AuthLogEntry entry) {
        if (writer == null) return;
        try {
            writer.write(gson.toJson(entry));
            writer.newLine();
            writer.flush();
            logHealthy.set(true);
        } catch (IOException e) {
            logHealthy.set(false);
            System.err.println("Error writing to auth.log: " + e.getMessage());
        }
    }

    public synchronized void rotate() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            Path current = Paths.get(logPath);
            if (Files.exists(current)) {
                Path backup = Paths.get(logPath + ".bak");
                Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            openWriter();
        } catch (IOException e) {
            System.err.println("Failed to rotate auth.log: " + e.getMessage());
        }
    }

    public synchronized void shutdown() {
        running.set(false);
        try {
            writerThread.join(1000);
            while (!queue.isEmpty()) {
                AuthLogEntry entry = queue.poll();
                if (entry != null) writeLine(entry);
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (Exception ignored) {}
    }
}
