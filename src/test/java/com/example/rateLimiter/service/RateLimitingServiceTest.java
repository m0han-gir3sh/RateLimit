package com.example.rateLimiter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    
    @Mock
    private DefaultRedisScript<Long> script;

    private FixedWindowRateLimiter fixedWindowRateLimiter;
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        fixedWindowRateLimiter=new FixedWindowRateLimiter(redisTemplate, script);
        tokenBucketRateLimiter=new TokenBucketRateLimiter(redisTemplate, script);
    }

    @Test
    void fixedBucket_shouldAllowRequestWhenScriptReturns1() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString()
        )).thenReturn(1L);
        boolean result=fixedWindowRateLimiter.isAllowed("user1");
        assertTrue(result);
    }

    @Test
    void fixedBucket_shouldBlockRequestWhenScriptReturns0() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString()
        )).thenReturn(0L);
        boolean result=fixedWindowRateLimiter.isAllowed("user1");
        assertFalse(result);
    }

    @Test
    void fixedBucket_shouldBlockIfRedisReturnsNull() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString()
        )).thenReturn(null);
        boolean result=fixedWindowRateLimiter.isAllowed("user1");
        assertFalse(result);
    }

    @Test
    void tokenBucket_shouldAllowRequestWhenScriptReturns1() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1L);

        boolean result=tokenBucketRateLimiter.isAllowed("user1");
        assertTrue(result);
    }

    @Test
    void tokenBucket_shouldBlockRequestWhenScriptReturns0() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(0L);

        boolean result=tokenBucketRateLimiter.isAllowed("user1");
        assertFalse(result);
    }

    @Test
    void tokenBucket_shouldBlockIfRedisReturnsNull() {
        when(redisTemplate.execute(
                eq(script),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(null);

        boolean result=tokenBucketRateLimiter.isAllowed("user1");
        assertFalse(result);
    }
}