package com.example.rateLimiter.service;

import com.example.rateLimiter.model.RateLimitStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterRouter {

    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    private final RateLimitStrategy strategy;

    public RateLimiterRouter(FixedWindowRateLimiter fixedWindowRateLimiter,
                             TokenBucketRateLimiter tokenBucketRateLimiter,
                             @Value("${rate.limiter.strategy}") String strategy) {

        this.fixedWindowRateLimiter=fixedWindowRateLimiter;
        this.tokenBucketRateLimiter=tokenBucketRateLimiter;
        this.strategy=RateLimitStrategy.valueOf(strategy);
    }

    public boolean isAllowed(String userId) {

        switch(strategy){
            case TOKEN_BUCKET:
                return tokenBucketRateLimiter.isAllowed(userId);
            case FIXED_WINDOW:
            default:
                return fixedWindowRateLimiter.isAllowed(userId);
        }
    }
}