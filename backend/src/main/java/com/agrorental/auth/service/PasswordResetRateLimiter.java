package com.agrorental.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Sliding-window limits for forgot-password requests, applied before any account lookup so the outcome is
 * identical for existing and non-existing accounts: at most {@code max-per-identifier} requests per
 * (role + normalized identifier) and {@code max-per-client} requests per client address per window.
 *
 * <p>State is in memory (per application instance). Keys are stored as SHA-256 hashes, so no identifier or
 * address is kept in clear text, and the number of tracked keys is bounded: when the bound is reached even
 * after evicting expired entries, new keys are refused (fail closed).
 */
@Component
public class PasswordResetRateLimiter {

    static final int MAX_TRACKED_KEYS = 50_000;

    private final int maxPerIdentifier;
    private final int maxPerClient;
    private final Duration window;
    private final Map<String, Deque<Instant>> hits = new HashMap<>();
    private Clock clock = Clock.systemUTC();

    public PasswordResetRateLimiter(
            @Value("${app.password-reset.rate-limit.max-per-identifier:5}") int maxPerIdentifier,
            @Value("${app.password-reset.rate-limit.max-per-client:20}") int maxPerClient,
            @Value("${app.password-reset.rate-limit.window-minutes:15}") long windowMinutes) {
        if (maxPerIdentifier < 1 || maxPerClient < 1 || windowMinutes < 1) {
            throw new IllegalArgumentException("Password reset rate limits must be positive");
        }
        this.maxPerIdentifier = maxPerIdentifier;
        this.maxPerClient = maxPerClient;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /**
     * Records one request and returns true when both limits allow it. A refused request is not recorded,
     * so callers that keep retrying do not extend their own lockout.
     */
    public synchronized boolean tryAcquire(String identifierKey, String clientKey) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        String idKey = "id:" + sha256(identifierKey);
        String clientHashKey = "client:" + sha256(clientKey);

        if (countRecent(idKey, cutoff) >= maxPerIdentifier || countRecent(clientHashKey, cutoff) >= maxPerClient) {
            return false;
        }
        if (hits.size() + 2 > MAX_TRACKED_KEYS) {
            evictExpired(cutoff);
            if (hits.size() + 2 > MAX_TRACKED_KEYS) {
                return false;
            }
        }
        hits.computeIfAbsent(idKey, k -> new ArrayDeque<>()).addLast(now);
        hits.computeIfAbsent(clientHashKey, k -> new ArrayDeque<>()).addLast(now);
        return true;
    }

    /** Fixed hint for the Retry-After header (identical for every refused request). */
    public long retryAfterSeconds() {
        return window.toSeconds();
    }

    /** Test support: forget all recorded requests. */
    public synchronized void clear() {
        hits.clear();
    }

    /** Test support: drive the window with a controllable clock. */
    public synchronized void setClock(Clock clock) {
        this.clock = clock;
    }

    synchronized int trackedKeyCount() {
        return hits.size();
    }

    private int countRecent(String key, Instant cutoff) {
        Deque<Instant> deque = hits.get(key);
        if (deque == null) {
            return 0;
        }
        while (!deque.isEmpty() && !deque.peekFirst().isAfter(cutoff)) {
            deque.pollFirst();
        }
        if (deque.isEmpty()) {
            hits.remove(key);
            return 0;
        }
        return deque.size();
    }

    private void evictExpired(Instant cutoff) {
        hits.entrySet().removeIf(entry -> {
            Deque<Instant> deque = entry.getValue();
            while (!deque.isEmpty() && !deque.peekFirst().isAfter(cutoff)) {
                deque.pollFirst();
            }
            return deque.isEmpty();
        });
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
