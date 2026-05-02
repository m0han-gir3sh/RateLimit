package com.example.rateLimiter.filters;

import com.example.rateLimiter.exception.ApiErrorResponseWriter;
import com.example.rateLimiter.service.RateLimiterRouter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimiterRouter service;
    private RateLimitingFilter filter;
    private ApiErrorResponseWriter errorResponseUtil;


    @BeforeEach
    void setup() {  
        service=mock(RateLimiterRouter.class);
        errorResponseUtil = mock(ApiErrorResponseWriter.class);
        filter = new RateLimitingFilter(service, errorResponseUtil);
    }

    @Test
    void allowRequest() throws Exception{
        HttpServletRequest req=mock(HttpServletRequest.class);
        HttpServletResponse res=mock(HttpServletResponse.class);
        FilterChain chain=mock(FilterChain.class);
        when(req.getHeader("userId")).thenReturn("user1");
        when(service.isAllowed("user1")).thenReturn(true);
        filter.doFilterInternal(req,res,chain);
        verify(chain).doFilter(req, res);
        verify(errorResponseUtil, never()).sendError(any(), anyInt(), anyString(), anyString());
    }

    @Test
    void rateLimitReturns429() throws Exception{
        HttpServletRequest req=mock(HttpServletRequest.class);
        HttpServletResponse res=mock(HttpServletResponse.class);
        FilterChain chain=mock(FilterChain.class);
        when(req.getHeader("userId")).thenReturn("user1");
        when(service.isAllowed("user1")).thenReturn(false);
        filter.doFilterInternal(req, res, chain);
        verify(res).setHeader(eq("Retry-After"), anyString());
        verify(chain,never()).doFilter(req, res);
        verify(errorResponseUtil).sendError(
                eq(res),
                eq(429),
                eq("Too Many Requests"),
                anyString()
        );
    }

    @Test
    void missingUserReturns401() throws Exception {
        HttpServletRequest req=mock(HttpServletRequest.class);
        HttpServletResponse res=mock(HttpServletResponse.class);
        FilterChain chain=mock(FilterChain.class);
        when(req.getHeader("userId")).thenReturn(null);
        filter.doFilterInternal(req,res,chain);
        verify(chain,never()).doFilter(req, res);
        verify(errorResponseUtil).sendError(
                eq(res),
                eq(401),
                eq("Unauthorized"),
                eq("User Id is missing")
        );
    }
}