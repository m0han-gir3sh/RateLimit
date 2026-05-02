package com.example.rateLimiter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class FixedWindowRateLimiter implements RateLimiter {

    @Value("${rate.limit.maxRequests}")
    private int maxRequests;

    @Value("${rate.limit.windowSeconds}")
    private int windowSeconds;

    private static final String KEY_PREFIX="rate_limit:";

    private final DefaultRedisScript<Long> script;
    private final StringRedisTemplate redisTemplate;

    public FixedWindowRateLimiter(StringRedisTemplate redisTemplate,@Qualifier("fixedWindowScript")DefaultRedisScript<Long> script){
        this.redisTemplate = redisTemplate;
        this.script = script;
    }

    @Override
    public boolean isAllowed(String userId) {

        String key=KEY_PREFIX+userId;
        //The entire script(INCR+EXPIRE+check) is executed as a single operation
        Long result=redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds)
        );
        return result!=null&&result==1;
    }
}