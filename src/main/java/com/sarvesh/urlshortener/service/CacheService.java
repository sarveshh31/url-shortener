package com.sarvesh.urlshortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.cache.url-ttl}")
    private long urlTtl;

    private static final String URL_PREFIX  = "url:";
    private static final String RATE_PREFIX = "rate:";

    public void cacheUrl(String shortCode, String originalUrl) {
        redisTemplate.opsForValue().set(
                URL_PREFIX + shortCode, originalUrl,
                Duration.ofSeconds(urlTtl));
    }

    public String getCachedUrl(String shortCode) {
        return redisTemplate.opsForValue().get(URL_PREFIX + shortCode);
    }

    public void evictUrl(String shortCode) {
        redisTemplate.delete(URL_PREFIX + shortCode);
    }

    public boolean isRateLimited(String ip,
                                 int maxRequests,
                                 long windowSeconds) {
        String key = RATE_PREFIX + ip;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000);

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1));

        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null && count > maxRequests;
    }
}