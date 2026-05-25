package com.gabaritaplus.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabaritaplus.api.dto.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.rate-limit.enabled}")
    private boolean enabled;

    @Value("${app.rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(Instant.now().getEpochSecond()));
        long currentMinute = Instant.now().getEpochSecond() / 60;
        if (counter.minute() != currentMinute) {
            counters.put(key, new WindowCounter(currentMinute));
            counter = counters.get(key);
        }

        if (counter.counter().incrementAndGet() > requestsPerMinute) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error("Limite de requisições excedido.", null));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record WindowCounter(long minute, AtomicInteger counter) {
        WindowCounter(long minute) {
            this(minute, new AtomicInteger(0));
        }
    }
}
