package com.example.rateLimiter.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenBucketRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX="token_bucket:";
    private static final String TOKENS_PER_MINUTE="100";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate,
                                   @Qualifier("tokenBucketScript")DefaultRedisScript<Long> script) {
        this.redisTemplate=redisTemplate;
        this.script=script;
    }

    @Override
    public boolean isAllowed(String userId) {

        String key=KEY_PREFIX + userId;

        Long result=redisTemplate.execute(
                script,
                List.of(key),
                TOKENS_PER_MINUTE,  
                String.valueOf(100.0/60.0),    
                String.valueOf(System.currentTimeMillis())
        );
        
        return result != null && result == 1;
    }
}