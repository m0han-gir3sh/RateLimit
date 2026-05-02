package com.example.rateLimiter.service;

public interface RateLimiter {
    boolean isAllowed(String userid);
}