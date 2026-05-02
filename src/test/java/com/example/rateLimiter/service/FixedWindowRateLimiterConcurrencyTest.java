package com.example.rateLimiter.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class FixedWindowRateLimiterConcurrencyTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2.4")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideRedisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private FixedWindowRateLimiter rateLimiter;

    @Test
    void shouldAllowOnly100RequestsUnderHighConcurrency() throws Exception {

        int totalRequests=200;
        int threadPoolSize=50;

        ExecutorService executor=Executors.newFixedThreadPool(threadPoolSize);

        AtomicInteger allowed=new AtomicInteger(0);
        AtomicInteger blocked=new AtomicInteger(0);

        CountDownLatch latch=new CountDownLatch(totalRequests);

        for (int i=0;i<totalRequests;i++) {
            executor.submit(()->{
                try{
                    boolean result=rateLimiter.isAllowed("user1");
                    if (result) {
                        allowed.incrementAndGet();
                    } else {
                        blocked.incrementAndGet();
                    }
                }finally{
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(100, allowed.get());
        assertEquals(100, blocked.get());
    }
}