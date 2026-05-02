package com.example.rateLimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {
    //lua script for fixed window
    public static final String FIXED_WINDOW=
        "local key=KEYS[1] " +
        "local limit=tonumber(ARGV[1]) " +
        "local window=tonumber(ARGV[2]) " +
        "local count=redis.call('INCR',key) " +
        "if count==1 then " +
        "redis.call('EXPIRE',key,window) " +
        "end " +
        "if count>limit then " +
        "return 0 " +
        "else " +
        "return 1 " +
        "end";
    //lua script for fixed window
    public static final String TOKEN_BUCKET_SCRIPT =
        "local key=KEYS[1] " +
        "local capacity=tonumber(ARGV[1]) " +
        "local refillRate=tonumber(ARGV[2]) " +
        "local now=tonumber(ARGV[3]) " +
        "local data=redis.call('HMGET', key, 'tokens', 'last') " +
        "local tokens=tonumber(data[1]) " +
        "local last=tonumber(data[2]) " +
        "if tokens==nil then " +
        "  tokens=capacity " +
        "  last=now " +
        "end " +
        "local delta=(now - last)/1000 " +
        "local refill=delta * refillRate " +
        "tokens=math.min(capacity, tokens + refill) " +
        "local allowed = 0 " +
        "if tokens >= 1 then " +
        "  tokens = tokens - 1 " +
        "  allowed = 1 " +
        "end " +
        "redis.call('HSET', key, 'tokens', tokens) " +
        "redis.call('HSET', key, 'last', now) " +
        "redis.call('EXPIRE', key, 120) " +
        "return allowed ";
        
    @Bean
    public DefaultRedisScript<Long> fixedWindowScript() {

        DefaultRedisScript<Long> script=new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(FIXED_WINDOW);

        return script;
    }

    @Bean
    public DefaultRedisScript<Long> tokenBucketScript() {
        DefaultRedisScript<Long> script=new DefaultRedisScript<>();
        script.setScriptText(TOKEN_BUCKET_SCRIPT);
        script.setResultType(Long.class);
        return script;
    }

}
